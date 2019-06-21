//***************************************************************************//
//            TrackLog                                                       //
//***************************************************************************//
function decimal_latlong(degs, mins, EWNS)
{
    var deg = parseFloat(degs);
    var min = parseFloat(mins);
    if ((EWNS == 'W') || (EWNS == 'S'))
      return -(deg + (min / 60));
    else
      return (deg + (min / 60));
}

function TrackLog(filestring, log_index)
{
    var self = this;
    self.log_index = log_index;	// index of this log. This value will be set such that
                    // this Tracklog = IGCview.logs[log_index]
    var baro_ok = false;
    var gps_ok = false;
    var gps_alt_ok=false; // these flags set to 'true' on good latitude and altitude 'B' records

    var date; // date from HFDTE record
    var name; // name from HFGID, HFCID records or file name

    // here are the arrays of data from the 'B' records, one set of elements per fix
    // the 'B' record can contain both GPS and Baro altitude.  IGCview stores two altitude arrays, the
    // primary (altitude[]) and alternate (alt_altitude[]).  Initially, baro alt is loaded into the 'altitude[]'
    // array, and GPS alt into the 'alt_altitude[]' array, and the 'gps_altitude' boolean is set to false.  If
    // the user selects 'GPS altitude', then the contents of the arrays will be switched, and the 'gps_altitude'
    // flag will be set to 'true'
    var time      = new Array();    // Time of day in seconds, alt in feet
    var altitude  = new Array();     // altitude (initially barometric) from 'B' records
    var alt_altitude  = new Array(); // alternate altitude (initially GPS) from 'B' records
    var latitude  = new Array();
    var longitude = new Array();  // W/S = negative, N/E = positive

    var GPS_ALT = 0,;
    var BARO_ALT = 1; // constants for GPS and BAROMETRIC altitude

    var altitude_type = BARO_ALT; // flag to specify whether gps (GPS_ALT) or baro (BARO_ALT) altitude is current
                        // if 'altitude_type' is 'GPS_ALT' then 'altitude[]' array contains GPS altitude
                        // otherwise it will contain Baro altitude
    var task = new Task();              // task from C records
    var record_count = 0;                              // number of records in log

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
    time[0] = 0;

    var file_lines = filestring.split('/n');

    for (var i=0; i<file_lines.length; i++)
    {
        var buf = file_lines[i];

        if (buf.length == 0) continue;

    if (!is_gardown && buf.charAt(0) == 'B')
    {
        // process 'B' records, the time/lat/long/alt records in the file
        // ignore all characters beyond the 35th
        if (buf.length < 35) continue; // skip record if not long enough
        is_igc = true;
        record_count++;
        latitude[record_count] = decimal_latlong(buf.substring(7,9),
                                                 buf.substring(9,11)+"."+buf.substring(11,14),
                                                 buf.charAt(14));
        if (latitude[record_count] != 0.0 && buf.charAt(24)=='V')
        {
            record_count--; // skip really bad B records (e.g. nz.igc)
            continue;
        }

        longitude[record_count] = decimal_latlong(buf.substring(15,18),
                                                  buf.substring(18,20)+"."+buf.substring(20,23),
                                                  buf.charAt(23));

        time[record_count] = parseInt(buf.substring(1,3)) * 3600 +
                             parseInt(buf.substring(3,5)) * 60 +
                             parseInt(buf.substring(5,7)) + day_offset;

        if (time[record_count] < time[record_count-1])
        {
            day_offset += 86400; // if time seems to have gone backwards, add a day
            time[record_count] += 86400;
        }

        altitude[record_count] = parseFloat(buf.substring(25,30)) * 3.2808 ; // Baro altitude
        alt_altitude[record_count] = parseFloat(buf.substring(30,35)) * 3.2808 ; // GPS altitude
        if (latitude[record_count]!=0.0) gps_ok=true;
        if (altitude[record_count]!=0.0) baro_ok=true;
        if (alt_altitude[record_count]!=0.0) gps_alt_ok=true;
    }
    if (!is_gardown && buf.charAt(0) == 'C')
    {
        is_igc = true;
        c_record_count++;
        if (c_record_count==3) last_tp = new TP(buf);
        else if (c_record_count>3)
            {
            int existing_tp = IGCview.tps.lookup(last_tp.trigraph);
            if (existing_tp>0)
                last_tp = IGCview.tps.tp[existing_tp];
            else
                IGCview.tps.add(last_tp); // add TP found in IGC file to TP_db
            task.add(last_tp);        // add previous TP to this logs task
            last_tp = new TP(buf);    // save this TP for subsequent add to task
            }
    }
    else if (!is_gardown && buf.startsWith("HFGID"))
    {
        is_igc = true;
        int name_pos = buf.indexOf(":");
        if (name_pos > 4 && name_pos+1 < buf.length())
        {
            name = buf.substring(name_pos+1);
            while (name.startsWith(" ")) name = name.substring(1);
        }
        // System.out.println("Using Glider ID <"+name+"> from IGC file");
    }
    else if (!is_gardown && buf.startsWith("HFCID"))
    {
        is_igc = true;
        int name_pos = buf.indexOf(":");
        if (name_pos > 4 && name_pos+1 < buf.length())
        {
            name = buf.substring(name_pos+1);
            while (name.startsWith(" ")) name = name.substring(1);
        }
    }
    else if (!is_gardown && buf.startsWith("HFDTE"))
    {
        is_igc = true;
        date = buf.substring(5);
    }
    else if (!is_igc && buf.startsWith("T  ") && buf.length()>47) // GARDOWN tracklog record
    {
        is_gardown = true;
        record_count++;
        if (record_count==IGCview.MAXLOG)
            {
            IGCview.msgbox("Too many records in "+filename+" (limit="+IGCview.MAXLOG+")");
            break;
            }
        latitude[record_count] = IGCview.decimal_latlong(buf.substring(4,6),
                                                    buf.substring(7,14),
                                                    buf.charAt(3));
        longitude[record_count] = IGCview.decimal_latlong(buf.substring(16,19),
                                                    buf.substring(20,27),
                                                    buf.charAt(15));
        time[record_count] = Integer.valueOf(buf.substring(39,41)).intValue()*3600 +
                                Integer.valueOf(buf.substring(42,44)).intValue()*60 +
                                Integer.valueOf(buf.substring(45,47)).intValue() + day_offset;
        if (date==null) date = buf.substring(36,38)+
                                IGCview.month_to_number(buf.substring(32,35))+
                                buf.substring(50);
        if (time[record_count]<time[record_count-1])
            {
            day_offset += 86400; // if time seems to have gone backwards, add a day
            time[record_count] += 86400;
            }
        altitude[record_count] =  0.0;
        if (latitude[record_count]!=0.0) gps_ok=true;
        if (altitude[record_count]!=0.0) baro_ok=true; // for future baro gps
        }
    }

    if (!baro_ok && gps_alt_ok)
    {
        baro_ok = true;
        set_altitude(GPS_ALT);
    }

    if (name==null) // if no HFGID or HFCID record then get name from filename
    {
        i = filename.lastIndexOf("/");
        if (i==-1) i = filename.lastIndexOf("\\");
        j = filename.lastIndexOf(".");
        if (j==-1) j = filename.length();
        name = filename.substring(i+1,j);
    }
    if (date==null) date = IGCview.get_date();
    in.close();
    calc_flight_data(); // initialize tracklog

}
