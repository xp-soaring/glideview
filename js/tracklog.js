//***************************************************************************//
//            TrackLog                                                       //
//***************************************************************************//

function TrackLog(filestring, log_index)
{
    var self = this;
    self.log_index = log_index;	// index of this log. This value will be set such that
                    // this Tracklog = IGCview.logs[log_index]
    var baro_ok = false;
    var gps_ok = false;
    var gps_alt_ok=false; // these flags set to 'true' on good latitude and altitude 'B' records

    self.date; // date from HFDTE record
    self.name; // name from HFGID, HFCID records or file name
    self.task = new Task();              // task from C records

    // here are the arrays of data from the 'B' records, one set of elements per fix
    // the 'B' record can contain both GPS and Baro altitude.  IGCview stores two altitude arrays, the
    // primary (altitude[]) and alternate (alt_altitude[]).  Initially, baro alt is loaded into the 'altitude[]'
    // array, and GPS alt into the 'alt_altitude[]' array, and the 'gps_altitude' boolean is set to false.  If
    // the user selects 'GPS altitude', then the contents of the arrays will be switched, and the 'gps_altitude'
    // flag will be set to 'true'

    var position = new Array(); // array of { time (int seconds), latitude, longitude, altitude, alt_altitude}

    var GPS_ALT = 0;
    var BARO_ALT = 1; // constants for GPS and BAROMETRIC altitude

    var altitude_type = BARO_ALT; // flag to specify whether gps (GPS_ALT) or baro (BARO_ALT) altitude is current
                        // if 'altitude_type' is 'GPS_ALT' then 'altitude[]' array contains GPS altitude
                        // otherwise it will contain Baro altitude

    var thermal = new Array();
    var thermal_count = 0;                              // number of thermals in log

    var climb_percent = new Array();
    var climb_avg = 0.0;
    var task_climb_avg =  0.0;
    var task_climb_height =  0.0;
    var task_climb_time = 0;
    var task_climb_percent =  0.0;
    var task_climb_count = 0;
    var task_time = 0;
    var task_distance =  0.0; // distance achieved around task (in nm)
    var task_speed =  0.0;
    var task_cruise_avg =  0.0;

    var tp_index  = new Array();     // index of log point at each TP
    var landed_index = 1;                                // index of log point on landout
    var tp_time  = new Array();      // time rounded each TP
    var tp_altitude = new Array(); // height rounded each TP (ft)

    // tp_latitude[tp_number] and tp_longitude[tp_number] are coordinates of turn
    //   if tp is an area turnpoint then these coordinates will be from a selected track point
    //   otherwise these coordinates will be the lat,long of the actual TP
    var tp_latitude = new Array();
    var tp_longitude = new Array();

    // these are the calculated performance stats for each leg
    var leg_time = new Array();        // duration for each leg (s)
    var leg_speed = new Array();     // speed for each leg (knots)
    var leg_climb_avg = new Array();      // average climb for each leg (knots)
    var leg_climb_percent = new Array();  // percent time climbing for each leg
    var leg_climb_count = new Array();      // percent time climbing for each leg
    var leg_cruise_avg = new Array();     // average cruise speed for each leg
    var leg_distance = new Array();  // achieved length of each actual leg. Differs from task
                                                // leg distances when area tp's are in the task
    var tps_rounded = 0;                                 // TPs successfully rounded

    var cursor2 = 0; // logpoint index of latest cursor2 setting from click on canvas - used by split()

    // note: this procedure reads records from filestring, and adds
    //   tracklog records as they are read, updating the 'date' and 'name' fields
    //   as appropriate records are found.  Only one procedure is needed for both
    //   gardown and .igc files, as the tracklog records "B..." and "T ..." are
    //   distinct in each file type.
    var is_igc = false;
    var is_gardown=false; // flags set when filetype is recognised
    var c_record_count = 0;  // skip the first 2 C records (preamble,takeoff)
                        // before using C records to build task.
                        // Also skip last C record (landing).
    var last_tp = new TP();  // saves previous TP found in 'C' record, as we don't want the final one

    var day_offset = 0;

    var time_begin;
    var time_end; // start and end time (in seconds of day) of flight
    // debug need to get sizes
    var baro_width = 150;
    var baro_height = 150;

    // switch between altitude[] and alt_altitude
  function set_altitude(alt_type)
  {
    var temp;
    if (alt_type==altitude_type) return;
    for (var i=1; i<=position.length; i++)
      {
        temp = position[i].altitude;
        position[i].altitude = position[i].alt_altitude;
        position[i].alt_altitude = temp;
      }
    altitude_type = alt_type;
  }

    var file_lines = filestring.split(/\r?\n/); // splitting string into lines (DOS or UNIX endings)

    console.log('Loading tracklog with',file_lines.length,'lines');

    for (var i=0; i<file_lines.length; i++)
    {
        var buf = file_lines[i];

        //console.log('Tracklog line: "'+buf+'"');

        if (buf.length == 0) continue;

        // Here is the all-important 'B' record in the IGC file which contains the logged position
        if (!is_gardown && buf.charAt(0) == 'B')
        {
            // process 'B' records, the time/lat/long/alt records in the file
            // ignore all characters beyond the 35th
            if (buf.length < 35) continue; // skip record if not long enough
            is_igc = true;
            var record = {};
            record.latitude = geo.decimal_latlong(buf.substring(7,9),
                                                    buf.substring(9,11)+"."+buf.substring(11,14),
                                                    buf.charAt(14));
            if (record.latitude != 0.0 && buf.charAt(24)=='V')
            {
                continue;
            }

            record.longitude = geo.decimal_latlong(buf.substring(15,18),
                                                    buf.substring(18,20)+"."+buf.substring(20,23),
                                                    buf.charAt(23));

            record.time = parseInt(buf.substring(1,3)) * 3600 +
                                parseInt(buf.substring(3,5)) * 60 +
                                parseInt(buf.substring(5,7)) + day_offset;

            if (record.latitude == 0.0 || record.longitude == 0.0 || record.time == 0)
            {
                continue;
            }

            if (position.length > 0 && record.time < position[position.length-1])
            {
                day_offset += 86400; // if time seems to have gone backwards, add a day
                record.time += 86400;
            }

            record.altitude = parseFloat(buf.substring(25,30)) * 3.2808 ; // Baro altitude
            record.alt_altitude = parseFloat(buf.substring(30,35)) * 3.2808 ; // GPS altitude
            if (record.latitude != 0.0) gps_ok=true;
            if (record.altitude != 0.0) baro_ok=true;
            if (record.alt_altitude != 0.0) gps_alt_ok=true;
            position.push(record);
            //console.log("Pushed position record",record);
        }

        // The IGC 'C' records contain the task turnpoints
        else if (!is_gardown && buf.charAt(0) == 'C')
        {
            is_igc = true;
            c_record_count++;
            if (c_record_count==3)
            {
                console.log("tracklog.js new TP("+buf+")");
                last_tp = new TP(buf);
                console.log("tracklog.js last_tp",last_tp.trigraph);
            }
            else if (c_record_count>3)
            {
                self.task.add(last_tp);        // add previous TP to this logs task
                console.log("tracklog.js new TP("+buf+")");
                last_tp = new TP(buf);    // save this TP for subsequent add to task
                console.log("tracklog.js last_tp",last_tp.trigraph);
            }
        }

        else if (!is_gardown && buf.startsWith("HFGID"))
        {
            is_igc = true;
            var name_pos = buf.indexOf(":");
            if (name_pos > 4 && name_pos+1 < buf.length)
            {
                self.name = buf.substring(name_pos+1);
                while (self.name.startsWith(" ")) self.name = self.name.substring(1);
            }
            console.log("Using Glider ID <"+self.name+"> from IGC file");
        }

        else if (!is_gardown && buf.startsWith("HFCID"))
        {
            is_igc = true;
            var name_pos = buf.indexOf(":");
            if (name_pos > 4 && name_pos+1 < buf.length)
            {
                self.name = buf.substring(name_pos+1);
                while (self.name.startsWith(" ")) self.name = self.name.substring(1);
            }
        }

        else if (!is_gardown && buf.startsWith("HFDTE"))
        {
            is_igc = true;
            self.date = buf.substring(5);
            console.log('Using tracklog date',self.date);
        }

        // Here we pick out the GARDOWN position records (i.e. the file is GARDOWN format)
        else if (!is_igc && buf.startsWith("T  ") && buf.length()>47) // GARDOWN tracklog record
        {
            is_gardown = true;
            var record = {};
            record.latitude = geo.decimal_latlong(buf.substring(4,6),
                                                        buf.substring(7,14),
                                                        buf.charAt(3));
            record.longitude = geo.decimal_latlong(buf.substring(16,19),
                                                        buf.substring(20,27),
                                                        buf.charAt(15));
            record.time = Integer.valueOf(buf.substring(39,41)).intValue()*3600 +
                                    Integer.valueOf(buf.substring(42,44)).intValue()*60 +
                                    Integer.valueOf(buf.substring(45,47)).intValue() + day_offset;
            if (record.latitude == 0.0 || record.longitude == 0.0 || record.time == 0)
            {
                continue;
            }
            if (self.date==null) self.date = buf.substring(36,38)+
                                    IGCview.month_to_number(buf.substring(32,35))+
                                    buf.substring(50);
            if (position.length > 0 && record.time < position[position.length-1].time)
            {
                day_offset += 86400; // if time seems to have gone backwards, add a day
                record.time += 86400;
            }
            record.altitude = 0.0;
            if (record.latitude != 0.0) gps_ok=true;
            if (record.altitude != 0.0) baro_ok=true; // for future baro gps
            position.push(record);
        }
    } // end for loop through tracklog lines

    if (!baro_ok && gps_alt_ok)
    {
        baro_ok = true;
        set_altitude(GPS_ALT);
    }

    if (self.name==null) // if no HFGID or HFCID record then get name from filename
    {
        i = filename.lastIndexOf("/");
        if (i==-1) i = filename.lastIndexOf("\\");
        j = filename.lastIndexOf(".");
        if (j==-1) j = filename.length();
        self.name = filename.substring(i+1,j);
    }

    self.draw = function(map, draw_options) {
        var options = Object.assign(config.tracklog_draw_options, draw_options);
        line_points = new Array(); //
        for (var i=0; i<position.length; i++)
        {
            line_points.push(new L.LatLng(position[i].latitude, position[i].longitude));
        }
        var polyline = new L.Polyline(line_points, options );
        polyline.addTo(map);
        if (options.fitBounds)
        {
            map.fitBounds(polyline.getBounds());
        }
    }; // draw()

    self.draw_baro = function(div, draw_options)
    {
        console.log("drawing baro");
        var canvas = document.createElement('CANVAS');
        div.appendChild(canvas);
        draw_time_alt(canvas.getContext('2d'));
    }

    function time_to_x(t)
    {
        return (t-time_begin) / (time_end - time_begin) * baro_width;
    }

    function alt_to_y(alt)
    {
        //debug max height hardcoded - should get from tracklog or config or draw call
        return (1 - alt / 3000) * 150
    }

    function draw_time_alt(ctx)
    {

      time_begin = position[0].time; // time (in seconds of day) of start of tracklog

      //debug - hardcoded duration of flight - need to pick up end time
      time_end = time_begin + 5*3600;

      ctx.beginPath()
      var x = time_to_x(position[0].time);
      var y = alt_to_y(position[0].altitude);
      ctx.moveTo(x,y);
      console.log("draw baro started at",x,y, position[0].altitude);
      for (var i = 1; i < position.length; i++)
        {
          x = time_to_x(position[i].time);
          y = alt_to_y(position[i].altitude);
          ctx.lineTo(x,y);
        }
        ctx.strokeStyle = "red";
ctx.stroke();
    }

//calc_flight_data(); // initialize tracklog

    return self;
}
