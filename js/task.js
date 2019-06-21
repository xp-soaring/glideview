//***************************************************************************//
//            Task                                                           //
//***************************************************************************//

function Task()
{
  int tp_count = 0;
  TP [] tp = new TP [IGCview.MAXTPS+1]; // first tp is tp[1], last is tp[tp_count]
  float [] dist = new float [IGCview.MAXTPS+1]; // dist[i] is length tp[i-1]..tp[i] in nautical miles
  float [] track = new float [IGCview.MAXTPS+1]; // track[i] is direction in degrees of tp[i-1]..tp[i]
  float [] bisector = new float [IGCview.MAXTPS+1]; // angle of bisector at TP (deg)
  boolean [] area = new boolean [IGCview.MAXTPS+1]; // boolean flag true for each TP in task defining an area
  float [] radius1 = new float [IGCview.MAXTPS+1]; // minimum distance (nm) for area around TP
  float [] radius2 = new float [IGCview.MAXTPS+1]; // maximum distance (nm) for area around TP
  int [] bearing1 = new int [IGCview.MAXTPS+1]; // minimum bearing (degrees) to TP for area
  int [] bearing2 = new int [IGCview.MAXTPS+1]; // maximum bearing (degrees) to TP for area

  float length = 0; // length of task in nautical miles
  float [] mid_latitude = new float [IGCview.MAXTPS+1]; // latitude of midpoint of leg[i]
  float [] mid_longitude = new float [IGCview.MAXTPS+1]; // longitude of midpoint of leg[i]
  int current_tp = 0; // number of current turnpoint being updated by igc_set_tp_info
  int current_taskpoint = 0; // index of taskpoint most recently selected
                             // taskpoints are the startpoint (1), midpoint of first leg (2), next TP (3)...
                             // so odd taskpoints are the tp's (including start/finish
                             // and even taskpoints are midpoints on each leg
  boolean extend_task = true; // flag set in task mode for special treatment of last TP (i.e extend task on drag)
  int a_x=0, a_y=0, b_x=0, b_y=0, c_x=0, c_y=0; // x,y coordinates of adjacent turnpoints to current turnpoint
                                                // used when mouse dragged to adjust task

  Task()
    {
      for (int i=0; i<=IGCview.MAXTPS; i++) area[i] = false; // default is 'turnpoint' rather than 'area'
    }

  void add(TP new_tp) // add new TP to end of task
    {
      set_tp(++tp_count, new_tp);
    }

  void insert_tp(int tp_num, TP new_tp) // insert a new TP in task as turnpoint tp_num (1=start)
    {
      if (tp_num==tp_count+1) // insert at end is equivalent to 'add'
        {
          add(new_tp);
          return;
        }
      for (int i=tp_count; i>=tp_num; i--)
        {
          tp[i+1] = tp[i]; // shuffle tp's up in task
          area[i+1] = area[i];
          radius1[i+1] = radius1[i];
          radius2[i+1] = radius2[i];
          bearing1[i+1] = bearing1[i];
          bearing2[i+1] = bearing2[i];
        }
      tp_count++;
      set_tp(tp_num, new_tp); // insert new TP
    }

  void delete_tp(int tp_num) // remove turnpoint number tp_num in task (1 = start)
    {
      for (int i=tp_num; i<tp_count; i++) // shuffle tp's down in task
        {
          tp[i] = tp[i+1]; // shuffle tp's up in task
          area[i] = area[i+1];
          radius1[i] = radius1[i+1];
          radius2[i] = radius2[i+1];
          bearing1[i] = bearing1[i+1];
          bearing2[i] = bearing2[i+1];
        }
      tp_count--;
      calc();

    }

  void set_tp(int tp_num, TP new_tp) // set TP in task at tp_num to new tp
    {
      tp[tp_num] = new_tp;
      current_taskpoint = 2*tp_num-1;
      area[tp_num] = false;
      radius1[tp_num] = (float) 0.0;
      radius2[tp_num] = (float) 0.0;
      bearing1[tp_num] = 0;
      bearing2[tp_num] = 0;
      calc();
    }

  void calc() // calculate derived values: midpoints, dists, tracks, bisectors
    {
      if (tp_count==0) return;
      if (tp_count==1)
        {
          track[1] = (float)0.0;
          dist[1] = (float)0.0;
          bisector[1] = (float)0.0;
          length = (float) 0.0;
          return;
        }
      // at least 2 TP's in task
      for (int i=1; i<tp_count; i++) // update values for all but last TP
        {
          track[i+1] = IGCview.dec_to_track(tp[i].latitude,
                                             tp[i].longitude,
                                             tp[i+1].latitude,
                                             tp[i+1].longitude);
          dist[i+1] = IGCview.dec_to_dist(tp[i].latitude,
                                             tp[i].longitude,
                                             tp[i+1].latitude,
                                             tp[i+1].longitude);
          mid_latitude[i] = (tp[i].latitude+tp[i+1].latitude)/2;
          mid_longitude[i] = (tp[i].longitude+tp[i+1].longitude)/2;
        }
      // calculate bisectors
      bisector[1] = (track[2] + (float)180.0) % 360;// start TP
      for (int i=2; i<tp_count; i++) // middle TP's
          bisector[i] = calc_bisector(track[i], track[i+1]);
      bisector[tp_count] = track[tp_count]; // finish TP

      // calculate length
      length = (float)0.0;
      for (int i=2;i<=tp_count;i++) length += dist[i];
    }

  // return latitude of taskpoint 'taskpoint'
  float taskpoint_lat(int taskpoint)
    {
      if (taskpoint % 2 == 1) // taskpoint is a turnpoint, rather than a leg mid-point
        return tp[taskpoint/2+1].latitude;
      else
        return mid_latitude[taskpoint/2];
    }

  // return longitude of taskpoint 'taskpoint'
  float taskpoint_long(int taskpoint)
    {
      if (taskpoint % 2 == 1) // taskpoint is a leg mid-point
        return tp[taskpoint/2+1].longitude;
      else
        return mid_longitude[taskpoint/2];
    }

  void set_adjacent_tps(int taskpoint) // set up a_x, a_y etc as screen x,y coordinates of adjacent turnpoints
                                   // to one (or midpoint) selected, for mouse drag task redraw
    {
      if (taskpoint==(2*tp_count-1)) // last TP selected, no task leg to redraw (task will be extended)
        {
         a_x = IGCview.canvas.long_to_x(taskpoint_long(taskpoint));
         a_y = IGCview.canvas.lat_to_y(taskpoint_lat(taskpoint));
         b_x = 0;
         b_y = 0;
         c_x = 0;
         c_y = 0;
        }
      else if (taskpoint==1) // start TP selected A=tp1, B=tp2
        {
         a_x = IGCview.canvas.long_to_x(taskpoint_long(1));
         a_y = IGCview.canvas.lat_to_y(taskpoint_lat(1));
         b_x = IGCview.canvas.long_to_x(taskpoint_long(3));
         b_y = IGCview.canvas.lat_to_y(taskpoint_lat(3));
         c_x = 0;
         c_y = 0;
        }
      else if (taskpoint%2==0) // taskpoint is a mid-point A=prevTP, B=nextTP
        {
         a_x = IGCview.canvas.long_to_x(taskpoint_long(taskpoint-1));
         a_y = IGCview.canvas.lat_to_y(taskpoint_lat(taskpoint-1));
         b_x = IGCview.canvas.long_to_x(taskpoint_long(taskpoint+1));
         b_y = IGCview.canvas.lat_to_y(taskpoint_lat(taskpoint+1));
         c_x = 0;
         c_y = 0;
        }
      else // taskpoint is a turnpoint A=prevTP, B=nextTP
        {
         a_x = IGCview.canvas.long_to_x(taskpoint_long(taskpoint-2));
         a_y = IGCview.canvas.lat_to_y(taskpoint_lat(taskpoint-2));
         b_x = IGCview.canvas.long_to_x(taskpoint_long(taskpoint));
         b_y = IGCview.canvas.lat_to_y(taskpoint_lat(taskpoint));
         c_x = IGCview.canvas.long_to_x(taskpoint_long(taskpoint+2));
         c_y = IGCview.canvas.lat_to_y(taskpoint_lat(taskpoint+2));
        }
    }

  boolean nearest_taskpoint(int x, int y)  // sets current_taskpoint = index of nearest task point to screen point x,y
                                        // 1 = start TP, 2 = midpoint of first leg, 3 = second TP...
                                        // called when mouse pressed in task_mode
                                        // UNLESS click was 20 pixels away, if so return false
    {
      float point_lat, point_long; // latitude and longitude of point clicked on canvas
      float current_lat=(float)0.0, current_long=(float)0.0; // latitude and longitude of nearest taskpoint
      int current_x, current_y; // screen x,y coordinates of nearest taskpoint
      int min_index = 1;
      float min_dist = 99999, dist;

      extend_task = true; // set up update_task() to ADD tp if last tp in task is selected

      if (tp_count==0) return false;
      point_lat = IGCview.canvas.y_to_lat(y);
      point_long = IGCview.canvas.x_to_long(x);
      for (int i=1; i<=tp_count*2-1; i++) // check through TP's
        {
          dist = IGCview.dec_to_dist(point_lat, point_long, taskpoint_lat(i), taskpoint_long(i));
          if (dist<min_dist)
            {
	            min_dist = dist;
              min_index = i;
            }
      	}
      current_x = IGCview.canvas.long_to_x(taskpoint_long(min_index));
      current_y = IGCview.canvas.lat_to_y(taskpoint_lat(min_index));
      if (((current_x-x)*(current_x-x)+(current_y-y)*(current_y-y))>400) return false;
      current_taskpoint = min_index;
      // if current_taskpoint a TP then also set current_tp_index in IGCview.tps
      if (current_taskpoint%2 == 1) IGCview.tps.current_tp_index = tp[current_taskpoint/2+1].index;
      set_adjacent_tps(current_taskpoint); // set up x,y coordinates of adjacent TP's
      return true;
    }

  void update_task(int x, int y) // called when mouse dragged in task mode
    {
      if (current_taskpoint==0) { show_task(); return; } // for some reason task TP not selected
      IGCview.tps.nearest_tp(x,y);
      if ((current_taskpoint % 2) == 1) // TPs in task are odd (1,3,5...), midpoints are even
        {
          if (current_taskpoint==(2*tp_count-1) && extend_task) // if clicked on finish tp then ADD new tp ONCE
            {
              extend_task = false; // only extend task ONCE during drag of mouse
              add(IGCview.tps.tp[IGCview.tps.current_tp_index]);
            }
          else // if clicked on start or middle tp, then MOVE tp
            set_tp((int) (current_taskpoint / 2)+1, IGCview.tps.tp[IGCview.tps.current_tp_index]);
        }
      else
        // if clicked on midpoint, then INSERT tp
        insert_tp((int) (current_taskpoint / 2)+1, IGCview.tps.tp[IGCview.tps.current_tp_index]);
      show_task();
    }

  void tidy_task() // called when mouse released in task mode
    {
      int i=1;
      while (i<tp_count)
        {
          if (tp[i]==tp[i+1]) delete_tp(i);
          else i++;
        }
    }

  // tp_index is the index of a turnpoint in IGCview.tps
  // set_current_tp() sets current_tp to the number of the task turnpoint corresponding that turnpoint
  // e.g. if MET is the second turnpoint on the task, and tp_index is tp #123 in the TP_db, then
  // tp_number(123) will set current_tp = 2
  void set_current_tp(int tp_index)
    {
      int i=1;
      current_tp = 0;
      while (i<=tp_count)
        if (tp[i].equals(IGCview.tps.tp[tp_index]))
          {
            current_tp = i;
            break;
          }
        else i++;
    }

  public void redraw_dragged(Graphics g) // redraw legs in task mode where taskpoint is being dragged
    {
      g.setColor(IGCview.config.MAPBACKGROUND.colour);
      if (b_x!=0 && b_y!=0)
          g.drawLine(a_x,a_y,b_x,b_y); // overwrite A-B line
      if (c_x!=0 && c_y!=0)
          g.drawLine(b_x,b_y,c_x,c_y); // overwrite B-C line
    }

  // draw task
  public void draw(Graphics g)
    {
      int x1,x2,y1,y2;
      if (tp_count==0) return;
      g.setColor(IGCview.config.TASKCOLOUR.colour);
      if (area[1]==false || IGCview.primary_index==0 || IGCview.logs[IGCview.primary_index].tps_rounded==0)
        {
          x1 = IGCview.canvas.long_to_x(tp[1].longitude);
          y1 = IGCview.canvas.lat_to_y(tp[1].latitude);
        }
      else // start area so draw task from actual lat/long of primary log start point
        {
          x1 = IGCview.canvas.long_to_x(IGCview.logs[IGCview.primary_index].tp_longitude[1]);
          y1 = IGCview.canvas.lat_to_y(IGCview.logs[IGCview.primary_index].tp_latitude[1]);
        }
      for (int i = 2; i <= tp_count; i++)
        {        // if not an area or primary log didn't reach area then draw line to center of TP
          if (area[i]==false || IGCview.primary_index==0 || i>IGCview.logs[IGCview.primary_index].tps_rounded)
            {
              x2 = IGCview.canvas.long_to_x(tp[i].longitude);
              y2 = IGCview.canvas.lat_to_y(tp[i].latitude);
            }
          else   // otherwise draw task line to actual log point in area
            {
              x2 = IGCview.canvas.long_to_x(IGCview.logs[IGCview.primary_index].tp_longitude[i]);
              y2 = IGCview.canvas.lat_to_y(IGCview.logs[IGCview.primary_index].tp_latitude[i]);
            }
          g.drawLine(x1,y1,x2,y2);
          if (IGCview.ApIGCview.draw_images_mode) // thicken lines if map images drawn underneath
            {
              g.drawLine(x1+1,y1,x2+1,y2);
              g.drawLine(x1,y1+1,x2,y2+1);
            }
          x1 = x2;
          y1 = y2;
        }
      draw_midpoints(g);
      if (IGCview.ApIGCview.task_mode) show_task();
    }

  public void draw_midpoints(Graphics g) // draw midpoint arrows on task leg
    {
      int x1,x2,y1,y2;
      g.setColor(IGCview.config.SECTORCOLOUR.colour);
      for (int i = 2; i <= tp_count; i++)
        {
          x1 = IGCview.canvas.long_to_x(mid_longitude[i-1]);
          y1 = IGCview.canvas.lat_to_y(mid_latitude[i-1]);
          IGCview.drawLineP(g,x1,y1,IGCview.task.track[i]+150,10); // draw midpoint arrow
          IGCview.drawLineP(g,x1,y1,IGCview.task.track[i]-150,10);
          IGCview.drawLineP(g,x1,y1,IGCview.task.track[i]+180,20);
        }
    }

  public void draw_task_tps(Graphics g)
    {
      g.setColor(IGCview.config.SECTORCOLOUR.colour);
      IGCview.tps.set_font(g); // set small font for tp names
      for (int i=1; i <= tp_count; i++)
        {
          IGCview.tps.mark_task_tp(g,tp[i]);
        }
    }

  void show_task() // build task description and send to summary side html frame (not task_info.html)
    {
      int taskpoint_index = 0; // counter to use to highlight current taskpoint

      String dist_units = "(nm)"; // set display string for distance units
      if (IGCview.config.convert_task_dist==IGCview.NMKM) dist_units = "(km)";
      else if (IGCview.config.convert_task_dist==IGCview.NMMI) dist_units = "(mi)";

      // build display table for task data
      String task_text = "<html><head><style> td { font-size: 9pt; font-family: sans-serif; }</style></head>";
      task_text = task_text + "<body leftmargin='2'>";
      task_text = task_text + "<table width='100%' border='0' cellpadding='1' cellspacing='0'>";
      task_text = task_text + "<tr><td><b>Task:</b></td></tr>";

      task_text = task_text + "<tr><td align='right'>" + dist_units + "</td></tr>";
      for (int i=1; i<=tp_count; i++)
        {
          if (i==1 || i==tp_count)
            task_text = task_text + "<tr><td bgcolor=#00ff00>" + tp[i].trigraph + "</td></tr>";
          else
            task_text = task_text + "<tr><td bgcolor=#00ffff>" + tp[i].trigraph + "</td></tr>";
          if (i<tp_count)
            task_text = task_text + "<tr><td align='right'>" + IGCview.places(dist[i+1]*IGCview.config.convert_task_dist,1) + "</td></tr>";
          else
            task_text = task_text + "<tr><td align='right'><b>" + IGCview.places(length*IGCview.config.convert_task_dist,1) + "</b></td></tr>";
        }
      task_text = task_text + "</table></body></html>";
      IGCview.show_task_data(task_text); // send text to javascript
    }

  void draw_areas(Graphics g, Drawable d)
    {
      for (int i=1; i<=tp_count; i++) draw_area(g,d,i);
    }

  void draw_area(Graphics g, Drawable d, int tp_number)
    {
      if (!area[tp_number]) return;
      float latitude = tp[tp_number].latitude;
      float longitude = tp[tp_number].longitude;
      int x = d.long_to_x(longitude);
      int y = d.lat_to_y(latitude);
      int tp_radius1 = IGCview.dist_to_pixels(d,latitude,longitude,radius1[tp_number]);
      int tp_radius2 = IGCview.dist_to_pixels(d,latitude,longitude,radius2[tp_number]);
      g.setColor(IGCview.config.SECTORCOLOUR.colour);
      // both bearing1 and bearing2 equal zero implies circular area
      if (bearing1[tp_number]==0 && bearing2[tp_number]==0)
        {
	        g.drawOval(x-tp_radius1,y-tp_radius1,2*tp_radius1,2*tp_radius1);
	        g.drawOval(x-tp_radius2,y-tp_radius2,2*tp_radius2,2*tp_radius2);
        }
      else
        {
          IGCview.drawLineP(g,x,y,bearing1[tp_number]+180,tp_radius2);
          IGCview.drawLineP(g,x,y,bearing2[tp_number]+180,tp_radius2);
          int arc_extent = Math.abs(360+bearing1[tp_number]-bearing2[tp_number]) % 360;
          g.drawArc(x-tp_radius1,
                    y-tp_radius1,
                    2*tp_radius1,
                    2*tp_radius1,
                    (630-bearing1[tp_number]) % 360,
                    arc_extent);
          g.drawArc(x-tp_radius2,
                    y-tp_radius2,
                    2*tp_radius2,
                    2*tp_radius2,
                    (630-bearing1[tp_number]) % 360,
                    arc_extent);
        }
    }

  void draw_sectors(Graphics g, Drawable d)
    {
      for (int i=1; i<=tp_count; i++) draw_sector(g,d,i);
    }

  void draw_sector(Graphics g, Drawable d, int tp_number)
    {
      if (area[tp_number]) return;
      float latitude = tp[tp_number].latitude;
      float longitude = tp[tp_number].longitude;
      int x = d.long_to_x(longitude);
      int y = d.lat_to_y(latitude);
      int tp_radius = IGCview.dist_to_pixels(d,latitude,longitude,IGCview.config.startline_radius);
      g.setColor(IGCview.config.SECTORCOLOUR.colour);
      if (tp_number==1)
        {
          if (IGCview.config.startline_180)
            {
              IGCview.drawLineP(g,x,y,IGCview.task.track[2]+90,tp_radius);
              IGCview.drawLineP(g,x,y,IGCview.task.track[2]-90,tp_radius);
              g.drawArc(x-tp_radius,y-tp_radius,2*tp_radius,2*tp_radius,
                        180 -(int)IGCview.task.track[2],
                        180);
            }
          else
            {
              IGCview.drawLineP(g,x,y,IGCview.task.track[2]+225,tp_radius);
              IGCview.drawLineP(g,x,y,IGCview.task.track[2]+135,tp_radius);
              g.drawArc(x-tp_radius,y-tp_radius,2*tp_radius,2*tp_radius,
                        225-(int)(IGCview.task.track[2]),
                        90);
            }
        }
      else if (tp_number==IGCview.task.tp_count)
        {
          g.setColor(IGCview.config.SECTORCOLOUR.colour.darker());
          IGCview.drawLineP(g,x,y,IGCview.task.track[IGCview.task.tp_count]+90,tp_radius);
          IGCview.drawLineP(g,x,y,IGCview.task.track[IGCview.task.tp_count]-90,tp_radius);
        }
      else
        {

          if (IGCview.config.beer_can_sectors)
            {
              tp_radius = IGCview.dist_to_pixels(d,latitude,longitude,IGCview.config.beer_tp_radius);
		          g.drawOval(x-tp_radius,y-tp_radius,2*tp_radius,2*tp_radius);
            }
          if (IGCview.config.photo_sectors)
            {
		          tp_radius = IGCview.dist_to_pixels(d,latitude,longitude,IGCview.config.photo_tp_radius);
              IGCview.drawLineP(g,x,y,IGCview.task.bisector[tp_number]+45,tp_radius);
              IGCview.drawLineP(g,x,y,IGCview.task.bisector[tp_number]-45,tp_radius);
              g.drawArc(x-tp_radius,y-tp_radius,2*tp_radius,2*tp_radius,
                        45-(int)(IGCview.task.bisector[tp_number]),
                        90);
            }
        }
    }

  boolean in_sector(float latitude, float longitude, int tp_index)
    {
      float bearing, margin, max_margin, bisector1, dist;

      if (latitude==(float)0.0) return false;  // cater for bad gps records
      dist = IGCview.dec_to_dist(tp[tp_index].latitude,
                                 tp[tp_index].longitude,
                                 latitude,
                                 longitude);
      if (tp_index == 1) // check if in start sector.  IGCview supports cylinder areas, start lines, and start photo sectors
        {
          if (area[1]) return (dist<radius2[1]); // detect inside start cylinder area if defined
          if (dist>IGCview.config.startline_radius) return false;
          bisector1 = track[2] + 180;
          if (bisector1 > 360) bisector1 -= 360;
          max_margin = (IGCview.config.startline_180) ? 90 : 45;
        }
      else if (tp_index == tp_count)
        {
          if (area[tp_count]) return (dist<radius2[tp_count]); // detect inside finish cylinder area if defined
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
              bearing = IGCview.dec_to_track(latitude,
                                             longitude,
                                             tp[tp_index].latitude,
                                             tp[tp_index].longitude);
              if (bearing1[tp_index]>bearing2[tp_index])
                return (bearing < (float) bearing1[tp_index]) && (bearing > (float) bearing2[tp_index]);
              return (bearing < (float) bearing1[tp_index]) || (bearing > (float) bearing2[tp_index]);
            }
          if ((IGCview.config.beer_can_sectors) && (dist<IGCview.config.beer_tp_radius))
            return true; // in sector if 'beer_can_sectors' && inside radius
          if (IGCview.config.photo_sectors == false)
            return false; // not in sector if no photo sector
          if (dist>IGCview.config.photo_tp_radius)
            return false; // not in sector if ouside photo sector radius
          bisector1 = bisector[tp_index];
          max_margin = 45;
	      }
      bearing = IGCview.dec_to_track(tp[tp_index].latitude,
                                     tp[tp_index].longitude,
                                     latitude,
                                     longitude);
      margin =  (bearing > bisector1) ? bearing - bisector1 : bisector1 - bearing;
      if (margin >= 180) margin = (float) 360 - margin;
      return (margin <= max_margin);
    }

  static float calc_bisector(float track_in, float track_out)
    {
      float bisector, bisector_offset;
      bisector = (track_in + track_out) / 2 + 90;
      bisector_offset = (bisector > track_in) ? bisector - track_in : track_in - bisector;
      if (bisector_offset > 90 && bisector_offset < 270) bisector += 180;
      if (bisector >= 360) bisector -= 360;
      return bisector;
    }

  boolean equals(Task t)
    {
      if (t==null) return false;
      if (t.tp_count!=tp_count) return false;
      for (int i=1;i<=tp_count;i++)
	      {
          if (!(tp[i].equals(t.tp[i]))) return false;
        }
      return true;
    }

  // is_AAT() returns true if task contains an AREA other than start/finish
  boolean is_AAT()
    {
      for (int tp_num=2; tp_num<tp_count; tp_num++) if (area[tp_num]) return true;
      return false;
    }
}
