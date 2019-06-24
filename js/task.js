//***************************************************************************//
//            Task                                                           //
//***************************************************************************//

function Task()
{
  self.tp_count = 0;
  var tp = new Array(); // first tp is tp[1], last is tp[var tp_count]
  var dist = new Array(); // dist[i] is length tp[i-1]..tp[i] in nautical miles
  var track = new Array(); // track[i] is direction in degrees of tp[i-1]..tp[i]
  var bisector = new Array(); // angle of bisector at TP (deg)
  var area = new Array(); // boolean flag true for each TP in task defining an area
  var radius1 = new Array(); // minimum distance (nm) for area around TP
  var radius2 = new Array(); // maximum distance (nm) for area around TP
  var bearing1 = new Array(); // minimum bearing (degrees) to TP for area
  var bearing2 = new Array(); // maximum bearing (degrees) to TP for area

  var length = 0; // length of task in nautical miles
  var mid_latitude = new Array(); // latitude of midpoint of leg[i]
  var mid_longitude = new Array(); // longitude of midpoint of leg[i]
  var current_tp = 0; // number of current turnpoint being updated by igc_set_tp_info
  var current_taskpoint = 0; // index of taskpoint most recently selected
                             // taskpoints are the startpoint (1), midpoint of first leg (2), next TP (3)...
                             // so odd taskpoints are the tp's (including start/finish
                             // and even taskpoints are midpoints on each leg
  var extend_task = true; // flag set in task mode for special treatment of last TP (i.e extend task on drag)

  self.add = function(new_tp) // add new TP to end of task
    {
      console.log("task.js adding TP",new_tp.trigraph);
      set_tp(++self.tp_count, new_tp);
      console.log("Added turnpoint",self.tp_count,new_tp.trigraph);
    }

  self.insert_tp = function(tp_num, new_tp) // insert a new TP in task as turnpoint tp_num (1=start)
    {
      if (tp_num==self.tp_count+1) // insert at end is equivalent to 'add'
        {
          self.add(new_tp);
          return;
        }
      for (var i=self.tp_count; i>=tp_num; i--)
        {
          tp[i+1] = tp[i]; // shuffle tp's up in task
          area[i+1] = area[i];
          radius1[i+1] = radius1[i];
          radius2[i+1] = radius2[i];
          bearing1[i+1] = bearing1[i];
          bearing2[i+1] = bearing2[i];
        }
      self.tp_count++;
      self.set_tp(tp_num, new_tp); // insert new TP
    }

  self.delete_tp = function(tp_num) // remove turnpoint number tp_num in task (1 = start)
    {
      for (var i=tp_num; i<self.tp_count; i++) // shuffle tp's down in task
        {
          tp[i] = tp[i+1]; // shuffle tp's up in task
          area[i] = area[i+1];
          radius1[i] = radius1[i+1];
          radius2[i] = radius2[i+1];
          bearing1[i] = bearing1[i+1];
          bearing2[i] = bearing2[i+1];
        }
      self.tp_count--;
      self.calc();
    }

  self.set_tp = function(tp_num, new_tp) // set TP in task at tp_num to new tp
    {
      tp[tp_num] = new_tp;
      current_taskpoint = 2*tp_num-1;
      area[tp_num] = false;
      radius1[tp_num] = 0.0;
      radius2[tp_num] = 0.0;
      bearing1[tp_num] = 0;
      bearing2[tp_num] = 0;
      self.calc();
    }

  self.calc = function() // calculate derived values: midpoints, dists, tracks, bisectors
    {
      if (self.tp_count==0) return;
      if (self.tp_count==1)
        {
          track[1] = 0.0;
          dist[1] = 0.0;
          bisector[1] = 0.0;
          length = 0.0;
          return;
        }
      // at least 2 TP's in task
      for (var i=1; i<self.tp_count; i++) // update values for all but last TP
        {
          track[i+1] = geo.dec_to_track(tp[i].latitude,
                                             tp[i].longitude,
                                             tp[i+1].latitude,
                                             tp[i+1].longitude);
          dist[i+1] = geo.dec_to_dist(tp[i].latitude,
                                             tp[i].longitude,
                                             tp[i+1].latitude,
                                             tp[i+1].longitude);
          mid_latitude[i] = (tp[i].latitude+tp[i+1].latitude)/2;
          mid_longitude[i] = (tp[i].longitude+tp[i+1].longitude)/2;
        }
      // calculate bisectors
      bisector[1] = (track[2] + 180.0) % 360;// start TP
      for (var i=2; i<self.tp_count; i++) // middle TP's
          bisector[i] = calc_bisector(track[i], track[i+1]);
      bisector[self.tp_count] = track[self.tp_count]; // finish TP

      // calculate length
      length = 0.0;
      for (var i=2;i<=self.tp_count;i++) length += dist[i];
    }

  // return latitude of taskpoint 'taskpoint'
  function taskpoint_lat(taskpoint)
    {
      if (taskpoint % 2 == 1) // taskpoint is a turnpoint, rather than a leg mid-point
        return tp[taskpoint/2+1].latitude;
      else
        return mid_latitude[taskpoint/2];
    }

  // return longitude of taskpoint 'taskpoint'
  function taskpoint_long(taskpoint)
    {
      if (taskpoint % 2 == 1) // taskpoint is a leg mid-point
        return tp[taskpoint/2+1].longitude;
      else
        return mid_longitude[taskpoint/2];
    }

  self.toHTML = function() // build task description and send to summary side html frame (not task_info.html)
    {
      var taskpoint_index = 0; // counter to use to highlight current taskpoint

      var dist_units = "(nm)"; // set display string for distance units
      if (config.convert_task_dist==geo.NMKM) dist_units = "(km)";
      else if (config.convert_task_dist==geo.NMMI) dist_units = "(mi)";

      // build display table for task data
      var task_text = "<table width='100%' border='0' cellpadding='1' cellspacing='0'>";
      task_text = task_text + "<tr><td><b>Task:</b></td></tr>";
      task_text = task_text + "<tr><td align='right'><b>"+dist_units+"</b></td></tr>";

      for (var i=1; i<=self.tp_count; i++)
        {
          if (i==1 || i==self.tp_count)
            task_text = task_text + "<tr><td bgcolor=#00ff00>" + tp[i].trigraph + "</td></tr>";
          else
            task_text = task_text + "<tr><td bgcolor=#00ffff>" + tp[i].trigraph + "</td></tr>";
          if (i<self.tp_count)
            task_text = task_text + "<tr><td align='right'>TP " +
                (dist[i+1]*config.convert_task_dist).toFixed(1) + "</td></tr>";
          else
            task_text = task_text + "<tr><td align='right'><b>" +
                (length*config.convert_task_dist).toFixed(1) + "</b></td></tr>";
        }
      task_text = task_text + "</table>";
      return task_text;
    }

  function in_sector(latitude,longitude, tp_index)
    {
      var bearing, margin, max_margin, bisector1, dist;

      if (latitude==0.0) return false;  // cater for bad gps records
      dist = geo.dec_to_dist(tp[tp_index].latitude,
                                 tp[tp_index].longitude,
                                 latitude,
                                 longitude);
      if (tp_index == 1) // check if in start sector.  IGCview supports cylinder areas, start lines, and start photo sectors
        {
          if (area[1]) return (dist<radius2[1]); // detect inside start cylinder area if defined
          if (dist>config.startline_radius) return false;
          bisector1 = track[2] + 180;
          if (bisector1 > 360) bisector1 -= 360;
          max_margin = (config.startline_180) ? 90 : 45;
        }
      else if (tp_index == self.tp_count)
        {
          if (area[self.tp_count]) return (dist<radius2[self.tp_count]); // detect inside finish cylinder area if defined
          bisector1 = track[tp_index];
          max_margin = 90;
        }
      else
        {
          if (area[tp_index])
            {
              // if outside distance limits then return false right away for efficiency...
              if (dist>radius2[tp_index] || dist<radius1[tp_index]) return false;
              // now must be within distance limits, so if no bearing limits return true
              if (bearing1[tp_index]==0 && bearing2[tp_index]==0)
                return true;
              // have bearing limits (but distance already known to be within radius1,2) so check bearing
              bearing = geo.dec_to_track(latitude,
                                             longitude,
                                             tp[tp_index].latitude,
                                             tp[tp_index].longitude);
              if (bearing1[tp_index]>bearing2[tp_index])
                return (bearing < bearing1[tp_index]) && (bearing > bearing2[tp_index]);
              return (bearing < bearing1[tp_index]) || (bearing > bearing2[tp_index]);
            }
          if ((beer_can_sectors) && (dist < config.beer_tp_radius))
            return true; // in sector if 'beer_can_sectors' && inside radius
          if (config.photo_sectors == false)
            return false; // not in sector if no photo sector
          if (dist > config.photo_tp_radius)
            return false; // not in sector if ouside photo sector radius
          bisector1 = bisector[tp_index];
          max_margin = 45;
	      }
      bearing = geo.dec_to_track(tp[tp_index].latitude,
                                     tp[tp_index].longitude,
                                     latitude,
                                     longitude);
      margin =  (bearing > bisector1) ? bearing - bisector1 : bisector1 - bearing;
      if (margin >= 180) margin = 360 - margin;
      return (margin <= max_margin);
    }

    function calc_bisector(track_in, track_out)
    {
      var bisector, bisector_offset;
      bisector = (track_in + track_out) / 2 + 90;
      bisector_offset = (bisector > track_in) ? bisector - track_in : track_in - bisector;
      if (bisector_offset > 90 && bisector_offset < 270) bisector += 180;
      if (bisector >= 360) bisector -= 360;
      return bisector;
    }

    self.equals = function(t) // task equality
    {
      if (t==null) return false;
      if (t.tp_count!=self.tp_count) return false;
      for (var i=1;i<=self.tp_count;i++)
	      {
          if (!(tp[i].equals(t.tp[i]))) return false;
        }
      return true;
    }

    // is_AAT() returns true if task contains an AREA other than start/finish
    self.is_AAT = function()
    {
      for (var tp_num=2; tp_num<self.tp_count; tp_num++)
      {
          if (area[tp_num]) return true;
      }
      return false;
    }

    // draw the task as a polyline on the map
    self.draw = function(map, draw_options)
    {
        var options = Object.assign(config.task_draw_options, draw_options);
        line_points = new Array(); //
        for (var i=1; i<tp.length; i++)
        {
            line_points.push(new L.LatLng(tp[i].latitude, tp[i].longitude));
        }
        var polyline = new L.Polyline(line_points, options );
        polyline.addTo(map);
        if (options.fitBounds)
        {
            map.fitBounds(polyline.getBounds());
        }
    }

    return self;

} // Task
