// Main class: IGCview
// Project type: console
// Arguments: 
// Compile command: javac -deprecation

/** (*) IGCview.java
 *  Soaring log file analysis
 *  @author Ian Lewis
 *  @version April 2003
 */

import java.applet.*;
import java.awt.*;
import java.awt.List;
import java.awt.event.*;
import java.net.*;  			// used for URL i/o
import java.io.*;
import java.util.*;
import java.math.*;
import java.awt.image.*;
import netscape.javascript.JSObject; 	// imported for calls to javascript functions

/*  General comments

Internally, IGCview uses feet, nautical miles, and knots as height, distance and speed units.
The units are converted on display to units selected by the user.

Track logs are stored in array IGCview.logs, so the nth tracklog loaded is in IGCview.logs[n].

The number of records in a given tracklog is in the log variable 'log.record_count'.

Each logpoint for time (s), altitude(ft), latitude, longitude are in:
    log.time[1..record_count]
    log.altitude[1..record_count]
    log.latitude[1..record_count]
    log.longitude[1..record_count]

 */

/* Changes for version 3.12 - Ian Forster-Lewis:

24/apr/2003: added appletFrame as parent for FileDialog for Java add-in (previous versions not compatible)

*/

/* Changes for version 3.11 - Ian Forster-Lewis:

21/apr/2003: if altitude in IGC file is only GPS, then read that (g7toWin files from Garmin 76s)

*/

/* Changes for version 3.10 - Ian Forster-Lewis:
28/jul/2002: landout can now be configured to max flight distance, not just dist to actual landed point
21/jul/2002: HTML tp_info window pops up when you click on a turnpoint on the map
             HTML task_info window to display task
             task can now include AREAS defined around turnpoints
14/jun/2002: added 'split' function to chop IGC log into two logs
31/may/2002: added ability to drag task around with mouse
             added 'task' button on top menu
             use cookie to store preferences, rather than old file code
             wrote a ton of help html files
30/mar/2002: added 'about' button to show version plus igc_about()

*/ 

/*
  Pedja's changes:

  * New panning and zooming behavior:
    - key navigation:
        home: zooms so that both track and task are in view
	pgup: zoom out
	pgdn: zoom in
	s: start of track (old home)
	e: end of track (old end)
	cursor up, down, left, right: pan N, S, W, E
	space: map cursor forward
	backspace: map cursor backward
    - map cursor is kept in-view when moved around

  * Various config parameters obtained from applet, rather (or in addition)
    to being read from config file.
    - unit measures, urls for track and tps to load

  * URLs for track and tp files can be absolute, rather than being
    relative to documentBase().

 */

/* Version 3 changes on version 2:

6/jan/2001: started modifying TP_db to allow call from javascript to load new TP's

4/nov/2000: added config option 'startup_load_tps' to avoid issue of loading file at time of
  'define task' javascript button click. javascript..java file protection is complex, so I try to
  keep file access from within the java applet.

24/oct/2000: removed MapFrame - DrawingCanvas now main map canvas
	removed CursorFrame - cursor updates now purely to HTML frame via CursorWindow

23/oct/2000: implemented clear_tracks

18/oct/2000: implemented is_primary
	allowed boxes around thermals for secondary logs

9/jan/2000: converted to APPLET (rather than standalone java with the JRE)
	AltGraph, ClimbGraph etc moved into IGCgraph applet
	IGCview now runs only as HTML page (IGCview.html) which loads IGCview.class and IGCgraph.class

2/jan/2000: defined zoom_to_flight method, plus auto_zoom on new track load

19/oct/99: photo+beercan sector now both possible
           legend in maggot race now offset from left window edge

 */

/* Version 2 changes on version 1:

20/feb/98: glider *icon* in maggot race
           config.glider_icons boolean added
 */

//***************************************************************************//
//             Config                                                        //
//***************************************************************************//

class ConfigVersion implements Serializable
{
  ConfigVersion() {};
  String version = "3.12";
}

class Config implements Serializable
{
  Config() {}; // constructor

  // WINDOWS
  int SELECTWIDTH = 400; // window default sizes
  int SELECTHEIGHT = 300;
  int SCORINGWIDTH = 780;
  int SCORINGHEIGHT = 500;
  int CONFIGWIDTH = 600;
  int CONFIGHEIGHT = 500;
  int TPWIDTH = 700;
  int TPHEIGHT = 600;
  int TPROWS = 3; // number of rows/columns in TP detail window
  int TPCOLS = 2;

  // MAP CONFIG
  boolean draw_pri_thermals = true;
  boolean draw_pri_thermal_ends = false;
  boolean draw_pri_thermal_strengths = true;
  boolean draw_sec_thermals = true;

  boolean draw_tp_times = true;
  boolean draw_tps = true;
  boolean draw_tp_names = true;
  boolean trim_logs = true;
  boolean upside_down_lollipops = false;
  boolean glider_icons = true;
  boolean draw_sectors = true;

  boolean draw_images = false; // load and draw map GIF's
  
  boolean beer_can_sectors = true;    // beercan sector (i.e. cylinder)
  boolean photo_sectors = true;    // 90 degree photo sector
  float beer_tp_radius = (float) 0.2699784; // nm = 500m
  float photo_tp_radius = (float) 1.6198704; // nm = 3km
  boolean startline_180 = true;        // perpendicular start line (vs. photo sector)
  float startline_radius = (float) 3.2397408; // nm = 3km
  boolean landout_max_distance = true; // true => landout point is at max flight distance,
                                       // rather than actual landed point
  
  float start_xscale = 200;  // startup scale and origin of map window
  float start_latitude = 53; // start in the UK somewhere by default
  float start_longitude = -1;
  float tp_xscale = 4000;    // startup scale of tp detail window

  boolean auto_zoom = true; // automatically calls 'zoom_to_flight' when new primary loaded

  // COLOURS
  ConfigColour MAPBACKGROUND = new ConfigColour(8,2); // ConfigColour(ColourIndex, ColourShade)
  ConfigColour CURSORCOLOUR = new ConfigColour(1,1);  // where ColourIndex is listed below
  ConfigColour SECTRACKCOLOUR = new ConfigColour(8,1);// and ColourShade: 0=darker, 1=lighter, 2=normal
  ConfigColour SECAVGCOLOUR = new ConfigColour(8,2);
  ConfigColour ALTCOLOUR = new ConfigColour(0,1);          // primary trace in ALT window
  ConfigColour ALTSECCOLOUR = new ConfigColour(8,2);       // secondary traces in ALT window
  ConfigColour TPBARCOLOUR = new ConfigColour(0,1);        // vertical lines in ALT windows
  ConfigColour TASKCOLOUR = new ConfigColour(1,1);         // task drawn on map
  ConfigColour TPTEXT = new ConfigColour(5,0);             // text colour of TP's on map
  ConfigColour TPCOLOUR = new ConfigColour(5,0);           // colour of TP's on map
  ConfigColour CLIMBCOLOUR = new ConfigColour(0,1);        // climb bars in CLIMB window
  ConfigColour CLIMBSECCOLOUR = new ConfigColour(8,2);     // secondary climb bars
  ConfigColour CRUISECOLOUR = new ConfigColour(0,1);       // speed bars in CRUISE window
  ConfigColour CRUISESECCOLOUR = new ConfigColour(8,2);    // secondary cruise bars
  ConfigColour SECTORCOLOUR = new ConfigColour(1,0);       // sector lines on tp detail window

  static final int MAXCOLOURS = 10;
  static final Color [] TRACKCOLOUR = { Color.blue,    // 0
                                        Color.red,     // 1
                                        Color.white,   // 2
                                        Color.cyan,    // 3
                                        Color.magenta, // 4
                                        Color.black,   // 5
                                        Color.pink,    // 6
                                        Color.yellow,  // 7
                                        Color.green,   // 8
                                        Color.orange   // 9
                                      };

	// FCOLOUR is index pair into TRACKCOLOUR for plane id in maggot race:
  static final int [][] FCOLOUR = {{0,0},{0,0},{1,1},{3,3},{4,4},{5,5},{6,6},
					     {7,7},{9,9},{0,1},{0,3},{0,7},{1,2},{1,3},
					     {1,7},{2,6},{2,7},{3,5},{3,6},{3,7},
					     {2,5},{9,6},{9,7},{9,5},{0,6},{9,7},
					     {4,5},{4,6},{4,7},{5,0},{5,1},{5,2},{5,3},{5,4},
					     {6,0},{6,1},{6,2},{6,3},{6,4},{6,5},{6,7},
					     {7,0},{7,1},{7,2},{7,3},{7,4},{7,5},{7,6}};

  static final int PRIMARYCOLOUR = 0; // INDEX of colour in TRACKCOLOUR for primary

  // tp database handling
  boolean startup_load_tps = true;	// load tp database file at startup

  // FILES
     //static String TPFILE = "G:\\src\\java\\igcview\\bga.gdn"; // TP gardown file
     //static String LOGDIR = "G:\\igcfiles\\grl97\\grl1\\";     // LOG directory
  String TPFILE = "turnpoints/bga.gdn"; // turnpoint file
  String LOGDIR = "."+System.getProperty("file.separator");
  String MAPFILE = "map_data/map_data.txt"; // image list text file
  
  // MAGGOT RACE PARAMETERS
  long RACEPAUSE = 100; // pause 100ms between screen updates
  boolean altsticks = true; // draw altitude stick below maggots

  // UNIT CONVERSIONS                      // Stored As:   Start Default: Conversion permitted:
  float convert_task_dist = IGCview.NMKM;  // NM           KM             1.0 NMKM(km) NMMI(mi)
  float convert_speed = (float) 1.0;       // KT           KT             1.0 NMKM(kmh) NMMI(mph)
  float convert_altitude = (float) 1.0;    // FT           FT             1.0 FTM(m)
  float convert_climb = (float) 1.0;       // KT           KT             1.0 KTMS(m/s)
  int time_offset = 0;                     // time zone adjustment
}

//***************************************************************************//
//             ConfigColour                                                  //
//***************************************************************************//

class ConfigColour implements Serializable
{
  Color colour;
  int index;
  int shade;

  ConfigColour(int index, int shade)
    {
      set(index, shade);
    }

  void set(int index, int shade)
    {
      this.colour = (shade==0) ? IGCview.config.TRACKCOLOUR[index].brighter() :
                                 ((shade==1) ? IGCview.config.TRACKCOLOUR[index] : 
                                               IGCview.config.TRACKCOLOUR[index].darker());
      this.index = index;
      this.shade = shade;
    }

  void set(ColourChoice c)
    {
      set(c.selected_colour(), c.selected_shade());
    }
}

//***************************************************************************//
//             IGCview                                                       //
//***************************************************************************//

public class IGCview extends Applet {

  // configuration objects

  static Config config = new Config();     // configuration
  static Config config_old = config;       // config backup
  static ConfigVersion config_version = new ConfigVersion();

  // application constants

  static String CONFIGFILE;                // defined in IGCview.html

  static final int   MAXTPS = 12;           // maximum TPs in Task
  static final int   MAXLOGS = 500;        // maximum track logs loaded
  static final int   MAXLOG = 20000;       // maximum log points in TrackLog
  static final int   MAXTHERMALS = 200;    // max number of thermals stored per log
  static final int   MAXTPDB = 10000;       // max number of TPs in database
  static final int   MAXIMAGES = 10000;     // max number of map images in ImageDisplayer

  // graph window constants (i.e. altitude, climb, cruise windows)

  static final int   BASELINE = 15;         // offset of zero axis from bottom of graph frame
                                           // maybe should be higher if legend to be included

  static final int   MAXALT = 40000;       // altitude window y-axis = 0..MAXALT feet
  static final int   ALTMAXDIST = 750;     // altitude window x-axis = 0..MAXALTDIST nm
  static final int   GRAPHTIME = 64800;    // altitude graph x-axis max is 18 hours  

  static final int   MAXCLIMB = 18;        // climb window 0..MAXCLIMB
  static final int   MAXCRUISE = 220;      // cruise window 0..MAXCRUISE

  static final int   CLIMBAVG = 30;        // averaging period for thermalling()
  static final int   CLIMBSPEED = 40;      // point-to-point speed => thermalling

  static final float TIMEXSCALE = (float) 0.04;  // scale for time x-axis in ALT windows
  static final float DISTXSCALE = (float) 4;  // scale: distance x-axis in ALT windows
  static final float DISTSTART = (float) 50;    // start offset for distance X-axis

  // 'landed' detection constants

  static final float LANDEDSPEED = (float) 4;      // landed if speed below 4 knots
  static final int   LANDEDAVG = 30;       //  averaged over 30 seconds
  static final int   LANDEDTIME = 120;     // without going faster for 2 minutes
  static final int   LANDEDBREAK = 240;    // or there's a break in the trace for 4 mins

  // 'climb profile' constants

  static final int   CLIMBBOXES = 30;      // number of categories of climb
  static final float CLIMBBOX = (float) 0.5;  // size of climb category in knots
  static final int   MAXPERCENT = 100;     // max %time in climb profile window
  static final int   LASTTHERMALLIMIT = 120; // ignore 'thermal' within 120secs of end


  static final int WINDTIME = 180; // use climbs longer than 3 mins to calc wind 

  static final private String appletInfo = 
    "IGC Soaring log file analysis program\nAuthor: Ian Forster-Lewis\nDecember 1997";

  // MAGGOT RACE CONSTANTS
  static final int RACE_TIME_INCREMENT = 7; // initial timestep increment for maggot race

  // GENERAL CONSTANTS
  static final int TIMEX=1, DISTX=2; // constants giving type of x-axis
  static final int GRAPHALT=1, GRAPHCLIMB=2, GRAPHCRUISE=3, GRAPHCP=4;

  static final float NMKM = (float) 1.852;  // conversion factor Nm -> Km
  static final float NMMI = (float) 1.150779;  // conversion factor Nm -> statute miles
  static final float FTM = (float) 0.3048;  // conversion factor Feet -> Meters
  static final float KTMS = (float) 0.5144;  // conversion factor Knot -> Meter/sec

  static private String usageInfo = "Usage: java IGCfile";

  static final String prefs_end = "}^"; // string to separate preferences in config cookie
  static final String prefs_eq = "^^";  // string used as 'preference equals'
                                        // so "pref1=value1;pref2=value2" coded as
                                        // pref1^^value1}^pref2^^value2}^

  // APPLICATION GLOBAL VARIABLES

  static TrackLog [] logs = new TrackLog[MAXLOGS+1];   // loaded track logs
                                                       // primary log = IGCview.logs[IGCview.primary_index]
  static TrackLogS sec_log = new TrackLogS();          // secondary track log
 
  static int log_count = 0;                      // count of logs loaded

  static int cursor1 = 0, cursor2 = 0;           // cursor indexes into primary log

  // WINDOWS
  static IGCview ApIGCview = null;               // main applet
  static Frame appletFrame = new Frame();        // parent frame for dialogs
  static DrawingCanvas canvas = null;            // main map window
  static int width, height;                      // size of main window
  static SelectFrame select_window = null;       // window for track select
  static ScoringFrame scoring_window = null;     // window for scoring data
  static ConfigFrame config_window = null;       // window for flight data
  static TPFrame tp_window = null;               // TP detail window

  // HTML WINDOW of frame containing <APPLET> tag for IGCview

  static JSObject html_window;

  // variables 'primary_index' and 'secondary[i]' identify primary and secondary logs

  static int primary_index = 0;                  // log number of primary flight
  static boolean [] secondary = new boolean [MAXLOGS+1]; // whether flight selected for
                                                 // secondary comparison

  // Turnpoints

  static TP_db tps = null; //new TP_db();                // tp database
  static boolean tps_loaded = false;             // set to true when TP file loaded

  // Task

  static Task task=null;                              // current task

  // Map image database object
  
  static Map_db maps = new Map_db();
  
  // globals to hold WIND information, calculated in 'calc_wind()'

  static float wind_speed=(float)0.0,
               wind_direction=(float)0.0;

  // globals used during the wind calculation

  static float thermal_x_speed_total, thermal_y_speed_total;
  static int wind_thermal_count;

  // global holding the Dispatcher thread

  static Dispatcher cmd_dispatcher;

  static String magic=null;
  
  boolean task_mode = false; // if set to TRUE then IGCview in task setting mode
                             // so tracks will not be drawn, and mouse moves will adjust task
                             
  boolean cursor_mode = false; // if set to TRUE then IGCview in cursor setting mode
                               // mouse click will set current cursor

  boolean add_to_task_mode = false; // set during igc_set_tp_info() if adding a new TP to the task
  
  boolean draw_images_mode = false; // set =config.draw_images during init()
                               
	//***********************************  STARTUP **************************************//
	//***********************************  STARTUP **************************************//
	//***********************************  STARTUP **************************************//

  public void init ()
   {
    ApIGCview = this;
    try {

      String m=getParameter("MAGIC");
      if (null!=m && null!=magic && m.length()>0 && m.equals(magic)) 
      {
	// NOTE: IGCview is not reentrant!!!
	// I.e., we cannot open two windows in one VM
	// ... way too many class variables
	// ... can use a bit of refactoring
	// Appart from these problems, the statics allow us
	// to avoid re-loading track and tp data if already loaded.
	// We use magic string to determine whether the currently
	// loaded track is the track that applet should run against.
	// I.e., the magic string is a unique id for the track/tp data.
	      magic=m;

      } else {
	      // Clear any previously loaded logs.
	      log_count=0;
        primary_index=0;
        task=null;
	      tps_loaded=false;
        tps=new TP_db();
	      cursor1=0;
        cursor2=0;
	      for(int i=1;i<=MAXLOGS;i++) secondary[i]=false;
      }

      String x;
      // Configure unit measures
      if(null!=(x=getParameter("UNIT_DISTANCE"))) {
	if(x.equals("nm"))
	  config.convert_task_dist=(float)1.0;
	else if(x.equals("mi") || x.equals("sm"))
	  config.convert_task_dist=NMMI;
	else if(x.equals("km"))
	  config.convert_task_dist=NMKM;
      }
      if(null!=(x=getParameter("UNIT_ALTITUDE"))) {
	if(x.equals("ft") || x.equals("feet"))
	  config.convert_altitude=(float)1.0;
	else if(x.equals("m")	|| x.equals("meters"))
	  config.convert_altitude=FTM;
      }
      if(null!=(x=getParameter("UNIT_SPEED"))) {
	if(x.equals("kts"))
     config.convert_speed=(float)1.0;
	else if(x.equals("mph"))
	  config.convert_speed=NMMI;
	else if(x.equals("kph"))
	  config.convert_speed=NMKM;
      }
      if(null!=(x=getParameter("UNIT_CLIMB"))) {
	if(x.equals("kts")
	   || x.equals("fpm"))
	  config.convert_climb=(float)1.0;
	else if(x.equals("mps"))
	  config.convert_climb=KTMS;
      }
   
          try { IGCview.CONFIGFILE = getParameter("CONFIGFILE"); }
          catch (NullPointerException e) { IGCview.CONFIGFILE = ""; }

          try { IGCview.width = Integer.valueOf(getParameter("WIDTH")).intValue(); }
          catch (NullPointerException e) { IGCview.width = 600; }

          try { IGCview.height = Integer.valueOf(getParameter("HEIGHT")).intValue(); }
          catch (NullPointerException e) { IGCview.height = 600; }

	        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
	        canvas = new DrawingCanvas();
	        this.add(canvas);
	        html_window = JSObject.getWindow(this);
	        cmd_dispatcher = new Dispatcher();
	        cmd_dispatcher.start();

		if(magic==m) {
		  // We got track and tp data already loaded
		  //canvas.paint(canvas.getGraphics());
		  canvas.zoom_to_home();
		} else {
		  // if (IGCview.CONFIGFILE.length()>1) load_config_file(IGCview.CONFIGFILE, true);
      load_config(); // load preferences from cookie
      
      // load map images 
      if (config.draw_images)
        {
          draw_images_mode = true;
          maps.load(config.MAPFILE);
        }
        
      // load turnpoint file if specified in querystring or in applet parameters 
		  String filename= querystring("tp_file");
      if (null!=filename && !filename.equals("")) tps.load(filename);
      else
        {
		      filename=getParameter("TURNPOINTS");
		      if(null!=filename) tps.load(filename);
          else if (IGCview.config.startup_load_tps) canvas.load_tps();
        }

      // load IGC file if specified in querystring or in applet parameters 
      String trackname = querystring("log_file");
		  if (null!=trackname && !trackname.equals("")) canvas.load_track_url(trackname);
      else
       {
         trackname = getParameter("TRACK");
		     if (null!=trackname) canvas.load_track_url(trackname);
       }
		  
      // load IGC files if 'log_list' specified in querystring or in applet parameters 
      String log_list_name = querystring("log_list");
		  if (null!=log_list_name && !log_list_name.equals("")) canvas.load_log_list(log_list_name);

		  magic=m; 
		}
        }
    catch (NumberFormatException e) { this.add (new TextField (appletInfo)); }
  }

  /*
  public void start()
    {
      IGCview.msgbox("start() called");
      // init();
    }
  */
  
  public String getAppletInfo () {
    return appletInfo;
  };

  // distance between two lat/longs in Nautical miles

  static float dec_to_dist(float lat1, float long1, float lat2, float long2)
    {
      double latR1, longR1, latR2, longR2;
      if (lat1 == lat2 && long1 == long2) return (float) 0;
      latR1 = lat1 * Math.PI / 180;
      longR1 = long1 * Math.PI / 180;
      latR2 = lat2 * Math.PI / 180;
      longR2 = long2 * Math.PI / 180;
      return (float) (Math.acos(Math.sin(latR1) * Math.sin(latR2) + 
                               Math.cos(latR1) * Math.cos(latR2) * Math.cos(longR1-longR2)) *
                     60.04042835 * 180 / Math.PI);
    }

  // track angle between two lat/longs in degrees, bearing from lat1,long1 -> lat2,long2

  static float dec_to_track(float lat1, float long1, float lat2, float long2)
    {
      float offset;
      if (lat1 == lat2 && long1 == long2) return (float) 0;
      offset = (float) (Math.atan( dec_to_dist(lat2, long1, lat2, long2) /
                                dec_to_dist(lat1, long1, lat2, long1)) * 180 / Math.PI);
      if (lat2 > lat1)
        if (long2 > long1) return offset; else return (float) 360 - offset;
      else
        if (long2 > long1) return (float) 180 - offset; else return (float) 180 + offset;        
    }

  // distance on map converted to pixels on screen

  static int dist_to_pixels(Drawable d, float latitude, float longitude, float dist)
    {
      return d.long_to_x(longitude + 
                         dist / dec_to_dist(latitude, longitude, latitude, longitude+1)) -
                         d.long_to_x(longitude);
    }

  String places(int i, int d) // extend int i to d decimal places, e.g. places(1,2) = "1.00"
    {
      return places((float)i, d);
    }

  static String places(float f, int d) // return string of f with d decimal places
    {
       if (d == 0) return String.valueOf( (int) (f+(float)0.5) );
       // else
       int factor = (int) java.lang.Math.pow(10,d); // 10 ^ d !!
       String p = String.valueOf( (float) ((int)(f*factor+(float)0.5)) / (float) factor);
       return (p+"0000000000").substring(0,p.indexOf('.')+d+1);
    }

  static String format_clock(int time)
    {
      if (time!=0) return format_time(time+IGCview.config.time_offset*3600);
      return "00:00:00";
    }

  static String format_time(int time)
    { int t = (time<0) ? time+86400 : time;
      int h = t / 3600;
      int m = (t-(h*3600)) / 60;
      int s = t - (h*3600) - (m*60);
      h = h % 24;
      String hh = two_digits(h);
      String mm = two_digits(m);
      String ss = two_digits(s);
      return hh+":"+mm+":"+ss;
    }

  // read lat or long from string and convert into float
  // buf must contain N,S,E or W, and also must contain a decimal point
  // e.g. N 52 06.4259 is ok
  // generally, a parsing error will return 0.0
  static float parse_latlong(String buf)
    {
      String digits = "0123456789";
      int point = buf.indexOf(".");
      if (point==-1) return (float) 0.0; // didn't find decimal point
      // collect minutes to left of decimal point
      int j = point-1;
      while (j>=0 && digits.indexOf(buf.charAt(j))!=-1) j--; // walk j to the left until it points to non digit
      if (point-j==1) return (float) 0.0; // didn't find any digits to left of "."
      String mins = buf.substring(j+1,point); // mins so far = integer minutes
      int i = point+1;
      while (i<buf.length() && digits.indexOf(buf.charAt(i))!=-1) i++; // walk i to the right until end or non digit
      if (i-point==1) return (float) 0.0; // didn't find any digits to right of "."
      mins = mins + "." +buf.substring(point+1,i);
      
      // collect degrees to left of integer minutes
      i = j;
      while (i>=0 && digits.indexOf(buf.charAt(i))==-1) i--; // walk i to the left until it points to digit
      if (i==-1) return (float) 0.0; // didn't find any degrees
      i++; // i now points to whitespace after degrees 
      j = i-1; // so j should point to right-most digit of degrees
      while (j>=0 && digits.indexOf(buf.charAt(j))!=-1) j--; // walk j to the left until it points to non digit
      if (i-j==1) return (float) 0.0; // didn't find any degree digits
      String degs = buf.substring(j+1,i); // degs = degrees
      
      // set EWNS string
      char EWNS = 'N';
      if (buf.indexOf("E")!=-1) EWNS = 'E';
      else if (buf.indexOf("W")!=-1) EWNS = 'W';
      else if (buf.indexOf("S")!=-1) EWNS = 'S';
      return decimal_latlong(degs, mins, EWNS);
    }
    
  static float decimal_latlong(String degs, String mins, char EWNS)
  {
    float deg = Float.valueOf(degs).floatValue();
    float min = Float.valueOf(mins).floatValue();
    if ((EWNS == 'W') || (EWNS == 'S')) 
      return -(deg + (min / (float) 60));
    else 
      return (deg + (min / (float) 60));
  }

  static String format_lat(float latitude)
    {
      String northing = (latitude<0) ? "S" : "N";
      String degrees = String.valueOf((int) Math.abs(latitude));
      String minutes = places((Math.abs(latitude) - ((int) Math.abs(latitude))) * 60,3);
      return northing+degrees+" "+minutes;
    }

  static String format_long(float longitude)
    {
      String easting = (longitude<0) ? "W" : "E";
      String degrees = String.valueOf((int) Math.abs(longitude));
      String minutes = places((Math.abs(longitude) - ((int) Math.abs(longitude))) * 60,3);
      return easting+degrees+" "+minutes;
    }

  static String two_digits(int i)
    {
      String cc = "00"+String.valueOf(i);
      return cc.substring(cc.length()-2);
    }

  static String three_digits(int i)
    {
      String cc = "000"+String.valueOf(i);
      return cc.substring(cc.length()-3);
    }

  static String five_digits(int i)
    {
      String cc = (i<0) ? "00000"+String.valueOf(-i) : "00000"+String.valueOf(i);
      return (i<0) ? "-"+cc.substring(cc.length()-4) : cc.substring(cc.length()-5);
    }

  static String make_title(TrackLog log)
    {
      if (log==null) return "";
      String title = "IGCview "+IGCview.config_version.version+": "+log.name+" "+IGCview.format_date(log.date)+" (vs. ";
      boolean no_secondaries = true;
      for (int i=1; i<=IGCview.MAXLOGS; i++)
        if (IGCview.secondary[i])
          {
            title = title + IGCview.logs[i].name + ", ";
            no_secondaries = false;
          }
      title = no_secondaries ? title.substring(0,title.length()-5) :	title.substring(0,title.length()-2)+")";
      return title;
    }

  static void exit()
    {
      System.exit(0);
    }

  static void calc_wind()
    {
      int log_num;
      thermal_x_speed_total = (float)0.0;
      thermal_y_speed_total = (float)0.0;
      wind_thermal_count = 0;
      for (log_num=1; log_num<=log_count; log_num++)
        if (log_num==primary_index || secondary[log_num])
          accumulate_wind(logs[log_num]);
      if (wind_thermal_count==0) return;
      wind_direction = (float) (Math.atan2(thermal_x_speed_total, thermal_y_speed_total) * 180 / Math.PI + 180);
      if (wind_direction<0) wind_direction += 360;
      if (wind_direction>=360) wind_direction -= 360;
      wind_speed = (float) Math.sqrt(thermal_x_speed_total * thermal_x_speed_total +
                                     thermal_y_speed_total * thermal_y_speed_total) /
                           wind_thermal_count;
    }

  static void accumulate_wind(TrackLog log)
    {
      int thermal_num;
      Climb climb;
      float thermal_x_speed, thermal_y_speed;
      int climb_duration;

      for (thermal_num=1; thermal_num<=log.thermal_count; thermal_num++)
        {
          climb = log.thermal[thermal_num];
          climb_duration = climb.duration();
          if (climb_duration<WINDTIME) continue;
          thermal_x_speed = dec_to_dist(log.latitude[climb.start_index],
                                      log.longitude[climb.start_index],
                                      log.latitude[climb.start_index],
                                      log.longitude[climb.finish_index]) / 
                          climb_duration * 3600;
          if (log.longitude[climb.finish_index]<log.longitude[climb.start_index])
            thermal_x_speed = -thermal_x_speed;
          thermal_y_speed = dec_to_dist(log.latitude[climb.start_index],
                                      log.longitude[climb.start_index],
                                      log.latitude[climb.finish_index],
                                      log.longitude[climb.start_index]) / 
                          climb_duration * 3600;
          if (log.latitude[climb.finish_index]<log.latitude[climb.start_index])
            thermal_y_speed = -thermal_y_speed;
          if (thermal_x_speed*thermal_x_speed+thermal_y_speed*thermal_y_speed > 2)
            {
              thermal_x_speed_total += thermal_x_speed;
              thermal_y_speed_total += thermal_y_speed;
              wind_thermal_count++;
            }
        }
    }

  // drawLine: Polar coords from x,y at angle theta for d pixels
  static void drawLineP(Graphics g, int x, int y, float theta, int d)
    {
      double theta_r = theta / 180 * Math.PI ; // theta in radians
      int x1 = (int) ((double) x + d * Math.sin(theta_r));
      int y1 = (int) ((double) y - d * Math.cos(theta_r));
      g.drawLine(x,y,x1,y1);
    }

  // trims "_" or " " from end of string (e.g. tp name) so "GRL____" becomes "GRL"
  // and replaces internal "_" with spaces (so html can split words across lines)
  static String trim_text(String s)
    {
      // trim underscores from end
      int i = s.length()-1;
      while (i>1 && s.charAt(i)=='_' || s.charAt(i)==' ') i--;
      // replace internal underscores with spaces
      String s1 = "";
      for (int j=0; j<s.length(); j++) s1 = s1 + ((s.charAt(j)=='_') ? " " : "" + s.charAt(j));  
      return s1;
    }
    
  static String month_to_number(String m)
    {
      if (m.startsWith("Jan")) return "01";
      if (m.startsWith("Feb")) return "02";
      if (m.startsWith("Mar")) return "03";
      if (m.startsWith("Apr")) return "04";
      if (m.startsWith("May")) return "05";
      if (m.startsWith("Jun")) return "06";
      if (m.startsWith("Jul")) return "07";
      if (m.startsWith("Aug")) return "08";
      if (m.startsWith("Sep")) return "09";
      if (m.startsWith("Oct")) return "10";
      if (m.startsWith("Nov")) return "11";
      if (m.startsWith("Dec")) return "12";
      return "XX";
    }

  static String number_to_month(String mm)
    {
      if (mm.startsWith("01")) return "Jan";
      if (mm.startsWith("02")) return "Feb";
      if (mm.startsWith("03")) return "Mar";
      if (mm.startsWith("04")) return "Apr";
      if (mm.startsWith("05")) return "May";
      if (mm.startsWith("06")) return "Jun";
      if (mm.startsWith("07")) return "Jul";
      if (mm.startsWith("08")) return "Aug";
      if (mm.startsWith("09")) return "Sep";
      if (mm.startsWith("10")) return "Oct";
      if (mm.startsWith("11")) return "Nov";
      if (mm.startsWith("12")) return "Dec";
      return "XXX";
    }

  static String format_date(String ddmmyy)
    {
      return ddmmyy.substring(0,2)+"."+number_to_month(ddmmyy.substring(2,4))+"."+
             ddmmyy.substring(4);
    }

  static String get_date()
    {
      Calendar c = Calendar.getInstance();
      int y = c.get(Calendar.YEAR);
      int m = c.get(Calendar.MONTH)+1;
      int d = c.get(Calendar.DAY_OF_MONTH);
      return two_digits(d)+two_digits(m)+two_digits(y);
    }

  static void new_task(Task new_task)
    {
      task = new_task;
      for (int i=1; i<=log_count; i++)
        {  
          if (i==primary_index || secondary[i])
            {
              logs[i].climb_avg = (float)0.0;
              logs[i].tps_rounded = 0;
              logs[i].calc_flight_data();
            }
        }
      wind_thermal_count = 0;
      wind_speed = (float)0.0;
      wind_direction = (float)0.0;
      canvas.paint(canvas.getGraphics());
    }

  static String convert_string(float f) // set string for preferences value for save in cookie
                                        // for dist,speed,climb,alt conversion factors
   {
    if (f==(float)1.0) return "1.0";
    else if (f==NMKM) return "NMKM";
    else if (f==NMMI) return "NMMI";
    else if (f==FTM) return "FTM";
    else if (f==KTMS) return "KTMS";
    //else
    IGCview.msgbox("IGCview error in converting " + f + " for preferences save");
    return "UNKNOWN CONVERSION";
   }
   
  static float convert_float(String s) // convert string read from preferences cookie back to float
                                       // for dist,speed,climb,alt conversion factors
   {
    if (s.equals("1.0")) return (float) 1.0;
    else if (s.equals("NMKM")) return NMKM;
    else if (s.equals("NMMI")) return NMMI;
    else if (s.equals("FTM")) return FTM;
    else if (s.equals("KTMS")) return KTMS;
    //else
    IGCview.msgbox("IGCview error in converting " + s + " for preferences load");
    return (float) -1;
   }
   
  static void save_config() // saves configuration preferences in cookie
   {                        // calls save_preferences to finally store data in cookie
    String prefs = prefs_end;
    prefs = prefs + "lat" + prefs_eq + TrackLog.lat_to_igc(canvas.zoom.latitude) + prefs_end;
    prefs = prefs + "long" + prefs_eq + TrackLog.long_to_igc(canvas.zoom.longitude) + prefs_end;
    prefs = prefs + "scale"+ prefs_eq + places(canvas.zoom.xscale,3) + prefs_end;
    prefs = prefs + "dist" + prefs_eq + convert_string(config.convert_task_dist) + prefs_end;
    prefs = prefs + "speed" + prefs_eq + convert_string(config.convert_speed) + prefs_end;
    prefs = prefs + "alt" + prefs_eq + convert_string(config.convert_altitude) + prefs_end;
    prefs = prefs + "climb" + prefs_eq + convert_string(config.convert_climb) + prefs_end;
    prefs = prefs + "tpfile" + prefs_eq + config.TPFILE + prefs_end;
    prefs = prefs + "logdir" + prefs_eq + config.LOGDIR + prefs_end;
    prefs = prefs + "time" + prefs_eq + config.time_offset + prefs_end;
    save_preferences(prefs);
    if (true) return;

    // here's the old file save stuff, now no longer used.
    FileDialog fd = new FileDialog (null, "Save configuration", FileDialog.SAVE);
    fd.setFile("*.cfg");
    fd.show ();
    String f = fd.getFile();
    if (f != null) {
      try {
        IGCview.config.start_latitude = IGCview.canvas.zoom.latitude;
        IGCview.config.start_longitude = IGCview.canvas.zoom.longitude;
        IGCview.config.start_xscale = IGCview.canvas.zoom.xscale;
        FileOutputStream s = new FileOutputStream (fd.getDirectory()+f);
        ObjectOutputStream o = new ObjectOutputStream (s);
        o.writeObject(IGCview.config_version);
        o.writeObject(IGCview.config);
        o.flush ();
        o.close ();
      }
      catch (Exception e) {System.out.println (e);};
    }
  }

  static void load_config()
    {
      String prefs = load_preferences(); // load preference string from cookie
      if (prefs==null || prefs.length()==0) return;
      int i = prefs.indexOf(prefs_end,0); // prefs always starts with the param separator
      if (i==-1)
        {
          IGCview.msgbox("IGCview strange error... prefs_end not found in config cookie");
          return;
        }
      i = i + prefs_end.length();
      while (i<prefs.length())
        {
          int j = prefs.indexOf(prefs_end,i); // i,j point to start, end+1 of next param string
          String current_pref = prefs.substring(i,j);
          int eq_index = current_pref.indexOf(prefs_eq);
          String current_param = current_pref.substring(0,eq_index);
          String current_value = current_pref.substring(eq_index+prefs_eq.length());

          if (current_param.equals("lat"))
            { 
              config.start_latitude = IGCview.decimal_latlong(current_value.substring(0,2),
                                                               current_value.substring(2,4)+"."+
                                                               current_value.substring(4,7),
                                                               current_value.charAt(7));
            }
          else if (current_param.equals("long"))
            { 
              config.start_longitude = IGCview.decimal_latlong(current_value.substring(0,3),
                                                                current_value.substring(3,5)+"."+
                                                                current_value.substring(5,8),
                                                                current_value.charAt(8));
            } 
          else if (current_param.equals("scale"))
            try
              {
                config.start_xscale =  Float.valueOf(current_value).floatValue();
              } catch (NumberFormatException nfe)
                 {
                   IGCview.msgbox("Error reading scale from preferences cookie");
                 }
          else if (current_param.equals("dist")) // distance units
            {
              config.convert_task_dist = convert_float(current_value); 
            }
          else if (current_param.equals("speed")) // speed units
            {
              config.convert_speed = convert_float(current_value); 
            }
          else if (current_param.equals("alt")) // altitude units
            {
              config.convert_altitude = convert_float(current_value); 
            }
          else if (current_param.equals("climb")) // rate of climb units
            {
              config.convert_climb = convert_float(current_value); 
            }
          else if (current_param.equals("tpfile")) // name of turnpoint file
            {
              config.TPFILE = current_value;
            }
          else if (current_param.equals("logdir")) // directory for igc files
            {
              config.LOGDIR = current_value;
            }
          else if (current_param.equals("time")) // local time offset from GMT
            try
              {
                config.time_offset = Integer.parseInt(current_value); 
              } catch (NumberFormatException nfe)
                 {
                   IGCview.msgbox("Error reading GMT time offset from preferences cookie");
                 }
          else 
            IGCview.msgbox("Unexpected value ("+current_param+") in your saved preferences");
          i = j + prefs_end.length();
        }
      canvas.zoom = new Zoom(IGCview.config.start_latitude,
                             IGCview.config.start_longitude,
                             IGCview.config.start_xscale);
	    canvas.paint(IGCview.canvas.getGraphics());

      // skip old file load stuff
      if (true) return;
      
      FileDialog fd = new FileDialog (null, "Load configuration", FileDialog.LOAD);
      fd.setFile("*.cfg");
      fd.show ();
      String f = fd.getFile();
      if (f != null)
			    load_config_file(fd.getDirectory()+f, false); // load file from filesystem
		}

  // load_config_file loads the 'config' object from a url or local file system file
  // this routine is not used in this version of IGCview.  A cookie is now used instead in load_config.
	static void load_config_file(String f, boolean is_url)
	  {
  		ObjectInputStream o = null;
      try
        {
          IGCview.config_old = IGCview.config;
	        if (is_url)
            {
		          URL log_url;
		          if(f.startsWith("http")) log_url=new URL(f);
		          else log_url= new URL(IGCview.ApIGCview.getDocumentBase(), f);
 	             o = new ObjectInputStream (log_url.openConnection().getInputStream());
						}
					else
					  {
	            o = new ObjectInputStream (new FileInputStream (f));
						}
          ConfigVersion c = (ConfigVersion) o.readObject();
          if (!c.version.substring(0,4).equals(IGCview.config_version.version.substring(0,4)))
            {
              IGCview.msgbox("Your "+IGCview.CONFIGFILE+" configuration file is the wrong version."+
                               " Using defaults.");
  	          IGCview.config = new Config();
              return;
            }
	        IGCview.config = (Config) o.readObject ();
	        o.close ();
          IGCview.canvas.zoom = new Zoom(IGCview.config.start_latitude,
                                                IGCview.config.start_longitude,
                                                IGCview.config.start_xscale);
	        IGCview.canvas.paint(IGCview.canvas.getGraphics());
        }
      catch (Exception e)
        {
	        IGCview.config = IGCview.config_old;
          System.out.println (e);
        }
    }

//******************************************************************************************//
//********** Java routines which call JavaScript *******************************************//
//** these are the *only* routines which call outside of IGCview.java to the surrounding   *//
//** page.  Generally the java routines call a javascript routine held in igcview.js.      *//
//******************************************************************************************//

  static void msgbox(String s)
    {
	    Object[] javascript_arg = { s };
	    html_window.call("igc_msgbox", javascript_arg);
    }

  static boolean confirm(String s)
    {
	    Object[] javascript_arg = { s };
	    return ((Boolean) html_window.call("igc_confirm", javascript_arg)).booleanValue();
    }

  static void show_status(String s)
    {
	    Object[] javascript_arg = { s };
	    html_window.call("igc_status", javascript_arg);
    }

  static void security_error(String s)
    {
	    Object[] javascript_arg = { s };
	    html_window.call("igc_security_error", javascript_arg);
    }

  static void show_cursor_data(String s)
    {
	    Object[] javascript_arg = { s };
	    html_window.call("igc_cursor_data", javascript_arg);
    }

  static void show_ruler_data(String s)
    {
	    Object[] javascript_arg = { s };
	    html_window.call("igc_ruler_data", javascript_arg);
    }

  static void show_flight_data(String s)
    {
	    Object[] javascript_arg = { s };
	    html_window.call("igc_flight_data", javascript_arg);
    }

  static void show_task_data(String s)
    {
	    Object[] javascript_arg = { s };
	    html_window.call("igc_task_data", javascript_arg);
    }

  static void save_preferences(String s)
    {
	    Object[] javascript_arg = { s };
	    html_window.call("igc_save_prefs", javascript_arg);
    }
    
  static String load_preferences()
    {
	    String prefs = (String) html_window.call("igc_load_prefs", null);
      return prefs;
    }
   
  // get querystring value with key s
  // current keys are:
  //    log_file: the http address of a log file
  //    log_list: the http address of a .txt file containing a list of http log file names
  //    tp_file: the http address of a turnpoint file in gardown format
  //    map_list: the http address of a .txt file containing map image names and coordinates
  static String querystring(String s)
    {
	    Object[] javascript_arg = { s };
	    String value = (String) html_window.call("igc_querystring", javascript_arg);
      return value;
    }
   
  // display tp html information window
  static void show_tp_info(int tp_index, boolean task_tp) // i is index of TP being displayed
                                                   // task_tp true if TP is in current task
    {
      Object[] javascript_arg = { String.valueOf(tp_index) };
      if (IGCview.task!=null) IGCview.task.set_current_tp(tp_index);
      if (task_tp) html_window.call("igc_show_task_tp_info", javascript_arg);
      else html_window.call("igc_show_tp_info", javascript_arg);
    }
    
  // display task html information window
  static void show_task_info()
    {
      html_window.call("igc_show_task_info", null);
    }
    
//***********************************************************************************************//
//********** UI mapping routines called by JavaScript *******************************************//
//***********************************************************************************************//

// These are the only java methods called from outside of the applet, i.e. from JavaScript.

// In some cases there are complimentary pairs of routines, 'igc_zzz_yyy' and 'jav_zzz_yyy'
// for example 'igc_file_open()' and 'jav_file_open()'.
// The JavaScript functions on the web page, typically associated with the onClick event of
// a button, *only* call the 'igc_zzz_yyy' user interface functions in this java program.
// The java security model provides limited access to files on the web server from which the
// applet has been loaded (i.e. an applet can read files from its own directory).  The Java
// virtual machine somehow keeps track of execution threads which are initiated by a call from
// JavaScript, and disallows file access in this case.  With IGCview, file loading initiated from
// an HTML button is a fundamental requirement, and to get around the security limitations of
// JavaScript I have implemented a simple disconnect between the JavaScript initiated thread and
// the java function calling thread.  See the class 'Dispatcher' for further
// explanation.

  public void igc_about() // display 'about' message including version
    {
      IGCview.msgbox("IGCview by Ian Forster-Lewis\nVersion "+IGCview.config_version.version);
    }

  public void igc_paint_map() // will repaint map view
    {
      canvas.paint(canvas.getGraphics());
    }
    
  public void igc_file_open()
    {
	    if (cmd_dispatcher.cmd_flag) return; // return if cmd_flag already set
	    cmd_dispatcher.cmd_name = "igc_file_open";
	    cmd_dispatcher.cmd_flag = true;
    }

  static public void jav_file_open() // this routine called by cmd_dispatcher
    {
	    canvas.load_track();
      canvas.paint(canvas.getGraphics());
    }

  public void igc_url_open(String filename)
    {
	    if (cmd_dispatcher.cmd_flag) return; // return if cmd_flag already set
	    cmd_dispatcher.cmd_name = "igc_url_open";
	    cmd_dispatcher.cmd_arg[1] = filename;
	    cmd_dispatcher.cmd_flag = true;
    }

  static public void jav_url_open(String filename) // this routine will be called by cmd_dispatcher
    {
	    canvas.load_track_url(filename);
      canvas.paint(canvas.getGraphics());
    }

  public void igc_load_images() // this routine is only called by Map_db.draw() when an image has been flagged for loading
    {
	    if (cmd_dispatcher.cmd_flag) return; // return if cmd_flag already set
	    cmd_dispatcher.cmd_name = "igc_load_images";
	    cmd_dispatcher.cmd_flag = true;
      cmd_dispatcher.wait_cmd(); // wait for command completion 
    }

  static public void jav_load_images() // this routine will be called by cmd_dispatcher
    {                                 // and will load all images flagged in Map array
      maps.load_images();
    }

  public void igc_load_mapfile(String filename)
    {
	    if (cmd_dispatcher.cmd_flag) return; // return if cmd_flag already set
	    cmd_dispatcher.cmd_name = "igc_load_mapfile";
	    cmd_dispatcher.cmd_arg[1] = filename;
	    cmd_dispatcher.cmd_flag = true;
      cmd_dispatcher.wait_cmd(); // wait for command completion 
    }

  static public void jav_load_mapfile(String filename) // this routine will be called by cmd_dispatcher
    {
	    maps.load(filename);
    }

  public void igc_save_as_igc()
    {
	    if (cmd_dispatcher.cmd_flag) return; // return if cmd_flag already set
	    cmd_dispatcher.cmd_name = "igc_save_as_igc";
	    cmd_dispatcher.cmd_flag = true;
    }

  static public void jav_save_as_igc()
    {
	    canvas.save_primary();
    }

  public void igc_load_preferences()
    {
	    if (cmd_dispatcher.cmd_flag) return; // return if cmd_flag already set
	    cmd_dispatcher.cmd_name = "igc_load_preferences";
	    cmd_dispatcher.cmd_flag = true;
    }

  static public void jav_load_preferences()
    {
	    load_config();
    }

  public void igc_save_preferences()
    {
	    if (cmd_dispatcher.cmd_flag) return; // return if cmd_flag already set
	    cmd_dispatcher.cmd_name = "igc_save_preferences";
	    cmd_dispatcher.cmd_flag = true;
    }

  static public void jav_save_preferences()
    {
	    save_config();
    }

  public void igc_load_tps(String filename)
    {
	    if (cmd_dispatcher.cmd_flag) return; // return if cmd_flag already set
	    cmd_dispatcher.cmd_name = "igc_load_tps";
	    cmd_dispatcher.cmd_arg[1] = filename;
	    cmd_dispatcher.cmd_flag = true;
    }

  static public void jav_load_tps(String filename)
    {
	    tps.load(filename);
      canvas.zoom.zoom_to_box(tps.lat_min, tps.long_min, tps.lat_max, tps.long_max);
      canvas.paint(canvas.getGraphics());
    }

  public void igc_clear_tracks()
    {
	    canvas.clear_tracks();
    }

  public void igc_select_tracks()
    {
      canvas.select_tracks();
    }

  public void igc_zoom_in()
    {
	    canvas.zoom_in();
    }

  public void igc_zoom_out()
    {
	    canvas.zoom_out();
    }

  public void igc_zoom_to_task()
    {
	    canvas.zoom_to_task();
    }

  public void igc_zoom_to_flight()
    {
	    canvas.zoom_to_flight();
    }

  public void igc_zoom_to_home() {canvas.zoom_to_home();}
  public void igc_pan_north() {canvas.pan_north();}
  public void igc_pan_east() {canvas.pan_east();}
  public void igc_pan_south() {canvas.pan_south();}
  public void igc_pan_west() {canvas.pan_west();}

  public void igc_zoom_reset()
    {
	    canvas.zoom_reset();
    }

  public void igc_ruler_zoom()
    {
	    canvas.ruler_zoom();
    }

  //public void igc_define_task()
  //  {
	//    canvas.get_task();
  //  }

  public void igc_set_wind()
    {
	    canvas.set_wind();
    }

  public void igc_synchro_start()
    {
      canvas.maggot_synchro();
    }

  public void igc_realtime_start()
    {
      canvas.maggot_real_time();
    }

  public void igc_race_stop()
    {
      canvas.maggot_cancel();
    }

  public void igc_race_forwards()
    {
      canvas.race_forwards();
    }

  public void igc_race_backwards()
    {
      canvas.race_backwards();
    }

  public void igc_race_pause()
    {
      canvas.race_pause();
    }

  public void igc_race_faster()
    {
      canvas.race_faster();
    }

  public void igc_race_slower()
    {
      canvas.race_slower();
    }

  public void igc_flight_data()
    {
      canvas.view_flight_data();
    }

  public void igc_scoring_data()
    {
      canvas.view_scoring();
    }

  public void igc_tp_detail()
    {
      canvas.view_tp_detail();
    }

  public void igc_preferences()
    {
      canvas.view_config();
    }

  // set 'task_mode' so next drag on map will draw first leg of new task
  public void igc_task_mode()
    {
      ApIGCview.task_mode = true; // set task_mode 
      // canvas.paint(canvas.getGraphics()); // repaint screen
    }

  // set 'cursor_mode' so next click on map will set current cursor position in primary trace
  public void igc_cursor_mode()
    {
      IGCview.ApIGCview.cursor_mode = true; 
    }

  // toggle 'draw_image_mode' and repaint
  public void igc_image_mode()
    {
      IGCview.ApIGCview.draw_images_mode = !IGCview.ApIGCview.draw_images_mode; 
      canvas.paint(canvas.getGraphics()); // repaint screen
    }

  public void igc_clear_task()
    {
      IGCview.task = null; 
      canvas.paint(canvas.getGraphics()); // repaint screen
    }

  // split primary log into two logs
  public void igc_split()
    {
      if (log_count>0)
        {
          if (logs[primary_index].cursor2==0)
            IGCview.msgbox("No cursor set in current log.  Log will be copied rather than split.");
          logs[primary_index].split();
          canvas.paint(canvas.getGraphics());
        }
      else IGCview.msgbox("No logs loaded.");
    }
    
  public String igc_search_tps(String key)
    {
      if (tps==null || tps.tp_count==0) return "No turnpoints loaded.";
      return tps.search(key);
    }
    
  // get data for passing to tp info window
  public String igc_get_tp_data(int tp_index, String field_name)
    {
      if (IGCview.tps==null) return "";
      if (field_name.equals("trigraph")) return IGCview.tps.tp[tp_index].trigraph;
      if (field_name.equals("full_name")) return IGCview.tps.tp[tp_index].full_name;
      if (field_name.equals("latitude")) return IGCview.format_lat(IGCview.tps.tp[tp_index].latitude);
      if (field_name.equals("longitude")) return IGCview.format_long(IGCview.tps.tp[tp_index].longitude);
      if (field_name.equals("dist_units"))
        {
          if (IGCview.config.convert_task_dist==(float)1.0) return "Nm";
          else if (IGCview.config.convert_task_dist==IGCview.NMKM) return "Km";
          else if (IGCview.config.convert_task_dist==IGCview.NMMI) return "Mi";
          else return "???";
        }
      if (field_name.equals("tp_or_area"))
        {
          if (IGCview.task==null || IGCview.task.current_tp==0)
            return "tp";
          else
            return (IGCview.task.area[IGCview.task.current_tp] ? "area" : "tp"); 
        }
      if (field_name.equals("radius1"))
        {
          if (IGCview.task==null || IGCview.task.current_tp==0)
            return "0";
          else 
            return places(IGCview.task.radius1[IGCview.task.current_tp]*IGCview.config.convert_task_dist,3); 
        }
      if (field_name.equals("radius2"))
        {
          if (IGCview.task==null || IGCview.task.current_tp==0)
            return String.valueOf(IGCview.places(IGCview.config.beer_tp_radius*IGCview.config.convert_task_dist,3));
          else
            return places(IGCview.task.radius2[IGCview.task.current_tp]*IGCview.config.convert_task_dist,3); 
        }
      if (field_name.equals("bearing1"))
        {
          if (IGCview.task==null || IGCview.task.current_tp==0)
            return "0";
          else
            return String.valueOf(IGCview.task.bearing1[IGCview.task.current_tp]);
        }
      if (field_name.equals("bearing2"))
        {
          if (IGCview.task==null || IGCview.task.current_tp==0)
            return "0";
          else
            return String.valueOf(IGCview.task.bearing2[IGCview.task.current_tp]);
        }
      IGCview.msgbox("IGCview error: igc_get_tp_info "+ field_name);
      return "";
    } 

  // set data in TP from tp info window
  public void igc_set_tp_data(int tp_index, String field_name, String value)
    {
      int task_tp_number;
      if (IGCview.tps==null) return;
      if (field_name.equals("add_to_task"))    IGCview.ApIGCview.add_to_task_mode = true;
      else if (field_name.equals("trigraph"))  IGCview.tps.tp[tp_index].trigraph = value;
      else if (field_name.equals("full_name")) IGCview.tps.tp[tp_index].full_name = value;
      else if (field_name.equals("latitude"))  IGCview.tps.tp[tp_index].latitude = IGCview.parse_latlong(value);
      else if (field_name.equals("longitude")) IGCview.tps.tp[tp_index].longitude = IGCview.parse_latlong(value);
      else if (field_name.equals("tp_or_area"))
        {
          if (value.equals("tp"))
            {
              if (IGCview.task!=null && IGCview.task.current_tp!=0)
                IGCview.task.area[IGCview.task.current_tp] = false;
              return;
            }
          if (IGCview.task==null || IGCview.task.tp_count==0)
            {
              IGCview.msgbox("This turnpoint must be in the task before an area can be defined");
              return;
            }
          if (IGCview.task.current_tp!=0) IGCview.task.area[IGCview.task.current_tp] = true;
        }
      else if (field_name.equals("radius1"))
        {
          if (IGCview.task==null || IGCview.task.tp_count==0) return;
          if (IGCview.task.current_tp!=0) IGCview.task.radius1[IGCview.task.current_tp] = Float.valueOf(value).floatValue() / IGCview.config.convert_task_dist;
        }
      else if (field_name.equals("radius2"))
        {
          if (IGCview.task==null || IGCview.task.tp_count==0) return;
          if (IGCview.task.current_tp!=0) IGCview.task.radius2[IGCview.task.current_tp] = Float.valueOf(value).floatValue() / IGCview.config.convert_task_dist;
        }
      else if (field_name.equals("bearing1"))
        {
          if (IGCview.task==null || IGCview.task.tp_count==0) return;
          if (IGCview.task.current_tp!=0) IGCview.task.bearing1[IGCview.task.current_tp] = Integer.valueOf(value).intValue();
        }
      else if (field_name.equals("bearing2"))
        {
          if (IGCview.task==null || IGCview.task.tp_count==0) return;
          if (IGCview.task.current_tp!=0) IGCview.task.bearing2[IGCview.task.current_tp] = Integer.valueOf(value).intValue();
        }
      else if (field_name.equals("done"))
        {
          IGCview.ApIGCview.add_to_task_mode = false;
          canvas.paint(canvas.getGraphics());
          if (IGCview.task==null || IGCview.task.tp_count==0) return;
          IGCview.task.current_tp = 0;
        }
      else IGCview.msgbox("Error in  igc_set_tp_info: "+field_name);
    } 
  
  // display task information window
  public void igc_task_info()
    {
      IGCview.show_task_info(); // will call javascript igc_show_task_info()
    }
    
  // get data for passing to tp info window
  public String igc_get_task_data(int tp_number, String field_name)
    {
      if (field_name.equals("dist_units"))
        {
          if (IGCview.config.convert_task_dist==(float)1.0) return "Nm";
          else if (IGCview.config.convert_task_dist==IGCview.NMKM) return "Km";
          else if (IGCview.config.convert_task_dist==IGCview.NMMI) return "Mi";
          else return "???";
        }
      if (field_name.equals("tp_count")) return (IGCview.task==null ? "0" : String.valueOf(IGCview.task.tp_count));
      if (IGCview.task==null) return "";
      if (field_name.equals("trigraph")) return IGCview.task.tp[tp_number].trigraph;
      if (field_name.equals("full_name")) return IGCview.task.tp[tp_number].full_name;
      if (field_name.equals("dist"))
        return ((tp_number>1) ? IGCview.places(IGCview.task.dist[tp_number]*IGCview.config.convert_task_dist,1) : "0");
      if (field_name.equals("track"))
        return ((tp_number>1) ? IGCview.places(IGCview.task.track[tp_number],0) : "0");
      if (field_name.equals("tp_or_area"))
        {
          if (IGCview.task==null)
            return "tp";
          else
            return (IGCview.task.area[tp_number] ? "area" : "tp"); 
        }
      if (field_name.equals("radius1"))
        {
          if (IGCview.task==null)
            return "0";
          else 
            return places(IGCview.task.radius1[tp_number]*IGCview.config.convert_task_dist,3); 
        }
      if (field_name.equals("radius2"))
        {
          if (IGCview.task==null)
            return String.valueOf(IGCview.places(IGCview.config.beer_tp_radius*IGCview.config.convert_task_dist,3));
          else
            return places(IGCview.task.radius2[tp_number]*IGCview.config.convert_task_dist,3); 
        }
      if (field_name.equals("bearing1"))
        {
          if (IGCview.task==null)
            return "0";
          else
            return String.valueOf(IGCview.task.bearing1[tp_number]);
        }
      if (field_name.equals("bearing2"))
        {
          if (IGCview.task==null)
            return "0";
          else
            return String.valueOf(IGCview.task.bearing2[tp_number]);
        }
      if (field_name.equals("length"))
        return IGCview.places(IGCview.task.length*IGCview.config.convert_task_dist,2);
      IGCview.msgbox("IGCview error: igc_get_task_info "+ field_name);
      return "";
    } 

  // set task data for each TP from task info window
  public void igc_set_task_data(int tp_number, String field_name, String value)
    {
      if (IGCview.task==null) return;
      if (field_name.equals("trigraph"))  IGCview.task.tp[tp_number].trigraph = value;
      else if (field_name.equals("full_name")) IGCview.task.tp[tp_number].full_name = value;
      // next two lines are not use currently - cannot set lat/long from task info window
      // else if (field_name.equals("latitude"))  IGCview.task.tp[tp_number].latitude = IGCview.parse_latlong(value);
      // else if (field_name.equals("longitude")) IGCview.task.tp[tp_number].longitude = IGCview.parse_latlong(value);
      else if (field_name.equals("tp_or_area")) IGCview.task.area[tp_number] = value.equals("area");
      else if (field_name.equals("radius1")) IGCview.task.radius1[tp_number] = Float.valueOf(value).floatValue() / IGCview.config.convert_task_dist;
      else if (field_name.equals("radius2")) IGCview.task.radius2[tp_number] = Float.valueOf(value).floatValue() / IGCview.config.convert_task_dist;
      else if (field_name.equals("bearing1")) IGCview.task.bearing1[tp_number] = Integer.valueOf(value).intValue();
      else if (field_name.equals("bearing2")) IGCview.task.bearing2[tp_number] = Integer.valueOf(value).intValue();
      else if (field_name.equals("done"))
        {
          if (IGCview.primary_index!=0) IGCview.logs[IGCview.primary_index].calc_flight_data();
          canvas.paint(canvas.getGraphics());
        }
      else IGCview.msgbox("Error in  igc_set_task_data: "+field_name);
    } 
  
  // insert new tp with index tp_index as turnpoint tp_num in task
  public void igc_insert_task_tp(int tp_num, int tp_index)
    {
      if (IGCview.task==null && tp_num==1) // create new task
        {
          IGCview.task = new Task();
        }
      if (IGCview.task==null ||
          IGCview.tps==null ||
          tp_index > IGCview.tps.tp_count ||
          tp_num > IGCview.task.tp_count+1) return;
      // an insert at task.tp_count+1 is equivalent to an 'add_tp' at end of task
      IGCview.task.insert_tp(tp_num, IGCview.tps.tp[tp_index]);
      // update primary log data
      if (IGCview.primary_index!=0)
        IGCview.logs[IGCview.primary_index].calc_flight_data();
    }
  
  // delete turnpoint tp_num in task
  public void igc_delete_task_tp(int tp_num)
    {
      if (IGCview.task==null ||
          IGCview.tps==null ||
          tp_num > IGCview.task.tp_count) return;
      IGCview.task.delete_tp(tp_num);
      // update primary log data
      if (IGCview.primary_index!=0)
        IGCview.logs[IGCview.primary_index].calc_flight_data();
    }
    
  // change turnpoint tp_num in task to new tp with index tp_index
  public void igc_change_task_tp(int tp_num, int tp_index)
    {
      if (IGCview.task==null ||
          IGCview.tps==null ||
          tp_index > IGCview.tps.tp_count ||
          tp_num > IGCview.task.tp_count) return;
      IGCview.task.set_tp(tp_num, IGCview.tps.tp[tp_index]);
      // update primary log data
      if (IGCview.primary_index!=0)
        IGCview.logs[IGCview.primary_index].calc_flight_data();
    }
    
}
//***************************************************************************//
//            Dispatcher                                                     //
//***************************************************************************//

// This class has one instance (IGCview.cmd_dispatcher).
// The purpose is to poll its 'cmd_flag' until this becomes true, and then
// call the routine specified in 'cmd_name'.

// The entire point is to have the IGCview routines called from within Java,
// even though the 'cmd_name' and parameters have been set up in a call from
// JavaScript.  In other words a JavaScript call to 'igc_url_open' is converted
// to a call to 'jav_url_open' from within a Java thread.  This is to allow
// file access from the applet *not* to appear as coming from a JavaScript action,
// as that is a security headache.

class Dispatcher extends Thread
{
  public String cmd_name;
  public String [] cmd_arg = new String[5]; // string argument list
  public boolean cmd_flag = false; // set to true when command ready for execution

  Dispatcher () {;}

  public void run()
    {
      try
        {
          while (true)
            {
              if (cmd_flag)
                {
                  if (cmd_name.equals("igc_url_open")) IGCview.jav_url_open(cmd_arg[1]);
                  else if (cmd_name.equals("igc_file_open")) IGCview.jav_file_open();
                  else if (cmd_name.equals("igc_load_images")) IGCview.jav_load_images();
                  else if (cmd_name.equals("igc_load_mapfile")) IGCview.jav_load_mapfile(cmd_arg[1]);
                  else if (cmd_name.equals("igc_save_as_igc")) IGCview.jav_save_as_igc();
                  else if (cmd_name.equals("igc_load_preferences")) IGCview.jav_load_preferences();
                  else if (cmd_name.equals("igc_save_preferences")) IGCview.jav_save_preferences();
                  else if (cmd_name.equals("igc_load_tps")) IGCview.jav_load_tps(cmd_arg[1]);
                  cmd_flag = false;
                }
              else
                  sleep(100); // sleep for 100 milliseconds
            }
        } catch (InterruptedException e) {;}
    }
    
  void wait_cmd() // synchronous wait for command completion
    {
      try
        {
          while (cmd_flag)
            {
              sleep(100); // sleep for 100 milliseconds
            }
        } catch (InterruptedException e) {;}
    }
} // end class Dispatcher

//***************************************************************************//
//            Drawable                                                       //
//***************************************************************************//

interface Drawable
{
  public int long_to_x(float longitude); // longitude to x coord
  public int lat_to_y(float latitude);  // latitude to y coord
}

//***************************************************************************//
//            DrawingCanvas                                                  //
//***************************************************************************//

class DrawingCanvas extends Canvas implements MouseListener,
 					                                    MouseMotionListener,
                                              KeyListener,
                                              RacingCanvas,
							                                Drawable
{
  Zoom zoom;
  int width, height;

  boolean racing = false;
  MaggotRacer maggot_racer;
  boolean ruler=false;          // flag set if canvas in 'ruler' mode rather than 'zoom' mode on mouse drag
  boolean zoom_this_once=false; // flag set if mouse clicked in task mode >20 pixels from task TP => should ZOOM
                                // rather than move that turnpoint in task

  int x0=0, y0=0;   // *always* x,y of mouse press (x,y may be adjusted, see below)
  int x=0, y=0;     // x,y of mouse press - IGCview will draw line/rectangle from x,y ->x1,y1
                    //   (and from x1,y1 to x2,y2 in task mode)
                    // note task_mousePressed() will adjust x,y to coords of preceding turnpoint
  int x1=0, y1=0;   // new draw x,y coordinates
  int x2=0, y2=0;   // x,y coordinates of second fixed point in task mode
  int prev_x = 0, prev_y = 0;   // old draw x,y coordinates
  int prev_x1 = 0, prev_y1 = 0;   // old draw x1,y1 coordinates

  DrawingCanvas ()
    {
      width = IGCview.width;
      height = IGCview.height;
      this.addMouseListener (this);
      this.addMouseMotionListener (this);
      this.addKeyListener (this);
      zoom = new Zoom(IGCview.config.start_latitude,
                      IGCview.config.start_longitude,
                      IGCview.config.start_xscale);
    }

  public int long_to_x(float longitude)
    {
      return (int) ((longitude - zoom.longitude) * zoom.xscale);
    }

  public int lat_to_y(float latitude)
    {
      return (int) ((zoom.latitude - latitude) * zoom.yscale);
    }
    
  void set_title()
    {
      String primary_title;
      if (IGCview.primary_index==0)
        primary_title = "";
      else
        primary_title = "Log: "+IGCview.make_title(IGCview.logs[IGCview.primary_index]);
      //setTitle("IGCview (Version "+IGCview.config_version.version+")  "+primary_title);
    }

  public void mousePressed (MouseEvent e)
    {
      x0 = e.getX();
      y0 = e.getY();
      x = x0;
      y = y0;
      x1 = x0;
      y1 = y0;
      if (task_mousePressed()) return;
      else if (ruler)
        {
          Graphics g = getGraphics();
          g.setColor(IGCview.config.MAPBACKGROUND.colour);
          g.setXORMode(Color.white);
          g.drawLine(prev_x, prev_y, prev_x1, prev_y1);  // remove old line
          prev_x = 0; // don't do a subsequent redraw of the line in update()
          prev_y = 0;
          prev_x1 = 0;
          prev_y1 = 0;
          update_ruler();
        }
    }

  boolean task_mousePressed() // mouse pressed at x,y - check for task_mode
                              // will return false if suitable taskpoint not found, either
                              // >20 pixels from nearest, or no TPs loaded
    {
      if (IGCview.tps==null) { IGCview.ApIGCview.task_mode = false; return false; }
      // if already in task_mode then pick nearest TP from tp database 
      if (IGCview.ApIGCview.task_mode)
        { if (IGCview.task==null) IGCview.task = new Task();
          if (IGCview.task.tp_count==0)
            {  // if task empty then select nearest TP to x,y as start point
              IGCview.tps.nearest_tp(x,y); // set tp database current_tp_index to nearest tp
              x1 = x;
              y1 = y;
              x = IGCview.canvas.long_to_x(IGCview.tps.tp[IGCview.tps.current_tp_index].longitude);
              y = IGCview.canvas.lat_to_y(IGCview.tps.tp[IGCview.tps.current_tp_index].latitude);
              // IGCview.show_status("Press: x="+x+";y="+y+";x1="+x1+";y1="+y1+";x2="+x2+";y2="+y2);
              Graphics g = getGraphics();
              g.setColor(IGCview.config.MAPBACKGROUND.colour);
              g.setXORMode(Color.white);
              // draw task line from nearest TP to click x,y
              g.drawLine(x, y, x1, y1);
              prev_x = x;
              prev_y = y;
              prev_x1 = x1;
              prev_y1 = y1;
              IGCview.task.add(IGCview.tps.tp[IGCview.tps.current_tp_index]); // add that tp to task as startpoint
              IGCview.task.show_task(); // display task in side window
              return true;
            }
        }
      // else if not already in task mode then:
      // if (no task) or (not clicked near a taskpoint)
      if (IGCview.task==null || !IGCview.task.nearest_taskpoint(x,y))
        {
          IGCview.ApIGCview.task_mode = false;
          IGCview.tps.nearest_tp(x,y); // set tp database current_tp_index to nearest tp
          return false;
        } // return false if no task or clicked >20 pixels from
          // turnpoint in current task
      // else (we have a task) and (we clicked near a taskpoint)
      // IGCview.msgbox("task_mousePressed at "+x+","+y);
      x1 = x; // set x1,y1 to mouse press
      y1 = y;                                                                             
      if (IGCview.task.current_taskpoint==1) // start TP selected so only draw one line
        {
          x = 0;
          y = 0;
        }
      else // set x,y coords to start of first line x,y -> x1,y1
        {
          x = IGCview.task.a_x;                                                                             
          y = IGCview.task.a_y;
        }
      if (IGCview.task.c_x!=0 && IGCview.task.c_y!=0) // if we have a C tp, then set x2,y2 to c_x,c_y
        {
          x2 = IGCview.task.c_x;                                                                                       
          y2 = IGCview.task.c_y;
        }
      else if (IGCview.task.b_x!=0 && IGCview.task.b_y!=0) // else if we have a B tp, then set x2,y2 to c_x,c_y                                                                                       
        {
          x2 = IGCview.task.b_x;                                                                                       
          y2 = IGCview.task.b_y;
        }
      else
        {
          x2 = 0;
          y2 = 0;
        }
      IGCview.task.redraw_dragged(getGraphics()); // erase task lines either side of click
      IGCview.task.show_task();
      // draw new task lines
      Graphics g = getGraphics();
      g.setColor(IGCview.config.MAPBACKGROUND.colour);
      g.setXORMode(Color.white);
      if (x!=0 && y!=0) g.drawLine(x, y, x1, y1);
      if (x2!=0 && y2!=0) g.drawLine(x1, y1, x2, y2);
      prev_x = x;
      prev_y = y;
      prev_x1 = x1;
      prev_y1 = y1;
      IGCview.ApIGCview.task_mode = true;
      return true;
    }
    
  public void mouseReleased (MouseEvent e)
    { 
      // IGCview.msgbox("mouseReleased x,y="+x+","+y+". x1,y1="+x1+","+y1+".");
      if (ruler) { reset_modes(); return; } // ruler mode only lasts for one measurement
      if ((x1-x0)*(x1-x0)<25 && (y1-y0)*(y1-y0)<25) // if movement less than 5 pixels, then treat as a 'click'
        {
          // IGCview.msgbox("mouseReleased < 5 pixels");
          if (IGCview.ApIGCview.cursor_mode) // if cursor_mode then set cursor to nearest primary point
            mouse_cursor(e.getX(), e.getY());
          else if (IGCview.ApIGCview.task_mode) // if task_mode then show tp info and repaint
            {
              // IGCview.msgbox("showing task TP");
              IGCview.show_tp_info(IGCview.tps.current_tp_index, true);
              paint(getGraphics());
            }
          else // otherwise show tp info
            {
              // IGCview.msgbox("show_tp_info for "+IGCview.tps.current_tp_index);
              IGCview.show_tp_info(IGCview.tps.current_tp_index, false);
            }
          reset_modes();
          return;
        }
      if (IGCview.ApIGCview.task_mode && !zoom_this_once)
        {
          IGCview.task.tidy_task(); // remove duplicate TP's
          reset_modes();
          if (IGCview.primary_index!=0) IGCview.logs[IGCview.primary_index].calc_flight_data();
          paint(getGraphics());
          return;
        }
      float x_adj = ((float) getSize().width / (float) (x1-x));
      float y_adj = ((float) getSize().height / (float) (y1-y));
      if (x_adj > y_adj) x_adj = y_adj;
      zoom.scale_corner(y_to_lat(y), x_to_long(x), x_adj);
      prev_x1 = 0;
      prev_y1 = 0;
      x1 = 0;
      y1 = 0;
      reset_modes();
      paint(getGraphics());
    }

    void reset_modes() // reset all the mode booleans which control behaviour for one mouse click 
                       // (called from mouseReleased)
      {
        IGCview.ApIGCview.task_mode = false;
        IGCview.ApIGCview.cursor_mode = false;
        zoom_this_once = false;
        ruler = false;
      }
      
  public void mouseDragged (MouseEvent e)
    {
      if (!zoom_this_once && IGCview.ApIGCview.task_mode)
        {
          task_mouseDragged(e);
          return;
        }
      x1 = e.getX();
      y1 = e.getY();
      if (x==0 && y==0) // fix for mousePressed just got focus 
        {
          x = x1;
          y = y1;
        }
      if (ruler) update_ruler();
      repaint();
    }

  public void task_mouseDragged(MouseEvent e) // mouse dragged in taskmode
    {
      x1 = e.getX();
      y1 = e.getY();
      // IGCview.show_status("Drag: x="+x+";y="+y+";x1="+x1+";y1="+y1+";x2="+x2+";y2="+y2);
      IGCview.task.update_task(x1, y1);
      Graphics g = getGraphics();
      g.setColor(IGCview.config.MAPBACKGROUND.colour);
      g.setXORMode(Color.white);
      // write over previous task lines
      if (x!=0 && y!=0) g.drawLine(x, y, prev_x1, prev_y1);
      if (x2!=0 && y2!=0) g.drawLine(prev_x1, prev_y1, x2, y2);
      // write new task lines
      if (x!=0 && y!=0) g.drawLine(x, y, x1, y1);
      if (x2!=0 && y2!=0) g.drawLine(x1, y1, x2, y2);
      prev_x1 = x1;
      prev_y1 = y1;
    }

  void update_ruler()
    {
      float lat1=y_to_lat(y),
            long1=x_to_long(x),
            lat2=y_to_lat(y1),
            long2=x_to_long(x1);
      RulerWindow.latitude = lat2;
      RulerWindow.longitude = long2;
      RulerWindow.dist = IGCview.dec_to_dist(lat1,long1,lat2,long2);
      RulerWindow.track = IGCview.dec_to_track(lat1,long1,lat2,long2);
      RulerWindow.update();
    }

  public void mouseClicked(MouseEvent e) {};
  public void mouseEntered(MouseEvent e) {};
  public void mouseExited(MouseEvent e) {};
  public void mouseMoved(MouseEvent e) {};

  void mouse_cursor(int x, int y)  // mouse clicked but not moved => set cursor
    {
      float cursor_lat, cursor_long;
      TrackLog log;
      Graphics g;
      int min_index = 1;
      float min_dist = 99999, dist;

      if (IGCview.primary_index==0) return;
      cursor_lat = y_to_lat(y);
      cursor_long = x_to_long(x);
      log = IGCview.logs[IGCview.primary_index];
      for (int i=1; i<=log.record_count; i++)
        {
          if (log.latitude[i]==(float)0.0) continue; // skip bad GPS records
          dist = IGCview.dec_to_dist(cursor_lat, cursor_long, log.latitude[i], log.longitude[i]);
          if (dist<min_dist)
            {
      	      min_dist = dist;
              min_index = i;
            }
      	}
      move_cursor1(min_index);
      while (++min_index<log.record_count &&log.latitude[min_index]==(float)0.0) {;} // skip bad recs
      move_cursor2(min_index);
    }

  public Dimension getPreferredSize () 
    {
      return new Dimension (width, height);
    }

  public Dimension getMinimumSize () 
    {
      return new Dimension (100,100);
    }

  public void update (Graphics g)
    {
      g.setColor(IGCview.config.MAPBACKGROUND.colour);
      g.setXORMode(Color.white);
      if (ruler)
        {
          if (prev_x1!=0) g.drawLine(prev_x, prev_y, prev_x1, prev_y1);  // remove old line
          g.drawLine(x, y, x1, y1);    // draw new line
          prev_x1 = x1;
          prev_y1 = y1;
          prev_x = x;
          prev_y = y;  // save dimensions of drawn line
        }
      else if ((x1-x) > 0) // else in zoom mode
        {
          g.drawRect(prev_x, prev_y, prev_x1-prev_x, prev_y1-prev_y);  // remove old rectangle
          g.drawRect(x, y, x1-x, y1-y);  // draw new rectangle
          prev_x1 = x1;
          prev_y1 = y1;
          prev_x = x;
          prev_y = y;  // save dimensions of drawn rectangle
        }
    }

  public void paint (Graphics g)
    { 
      x = 0; // reset ruler/zoom box coordinates
      y = 0;
      x1 = 0;
      y1 = 0;
      prev_x = 0;
      prev_y = 0;
      prev_x1 = 0;
      prev_y1 = 0;
      Dimension d = getSize();
      g.setColor(IGCview.config.MAPBACKGROUND.colour);
      g.fillRect(0,0, d.width - 1, d.height - 1);
      if (IGCview.ApIGCview.draw_images_mode)
        {
          IGCview.maps.draw(g, this);
        }
      if (IGCview.task!=null) IGCview.task.draw(g);
      if (IGCview.tps!=null) IGCview.tps.draw(g);
      if (IGCview.task!=null && IGCview.config.draw_sectors) IGCview.task.draw_sectors(g,this);
      if (IGCview.task!=null) IGCview.task.draw_areas(g,this); // draw task turnpoint areas
      if (IGCview.task!=null) IGCview.task.draw_task_tps(g); // highlight turnpoints
      if (racing)
        {
          draw_legend(g);
          return;
        }
      // if (!IGCview.ApIGCview.task_mode) 
      for (int i = 1; i <= IGCview.log_count; i++) // draw secondary logs
        {                                                                            // if not in task mode
          // System.err.println("Log "+i+" Sec="+IGCview.secondary[i]+" IGCview.logs[i]="+
          //                   ((IGCview.logs[i]==null) ? "null" : "good pointer"));
          if (IGCview.secondary[i] && IGCview.logs[i]!=null)
		        {
              IGCview.logs[i].draw(this,g,i);
		          if (IGCview.config.draw_sec_thermals) IGCview.logs[i].draw_thermals(this, g);
		        }
        }
      // if (!IGCview.ApIGCview.task_mode && IGCview.primary_index>0) // draw primary log unless in task mode
      if (IGCview.primary_index>0) // draw primary log unless in task mode
        {
          IGCview.logs[IGCview.primary_index].draw(this, g, IGCview.config.PRIMARYCOLOUR);
          IGCview.logs[IGCview.primary_index].draw_thermals(this, g);
          IGCview.logs[IGCview.primary_index].draw_tp_times(this, g);
          draw_cursors(g);
        }
    }

  void zoom_in()
    {
      cancel_ruler();
      zoom.zoom_in();
      paint(getGraphics());
    }

  void zoom_out()
    {
      cancel_ruler();
      zoom.zoom_out();
      paint(getGraphics());
    }

  void zoom_to_task()
    {
      cancel_ruler();
      zoom.zoom_to_task();
      paint(getGraphics());
    }

  void zoom_to_flight()
    {
      cancel_ruler();
      zoom.zoom_to_flight();
      paint(getGraphics());
    }

  void zoom_to_home()
    {
      cancel_ruler();
      zoom.zoom_to_home();
      paint(getGraphics());
    }

  void zoom_reset()
    {
      cancel_ruler();
      zoom.zoom_reset();
      paint(getGraphics());
    }

  void pan_north()
  {
    cancel_ruler();
    zoom.pan((float)(0.25*IGCview.height/zoom.yscale),(float)0.0);
    paint(getGraphics());
  }
  
  void pan_east()
  {
    cancel_ruler();
    zoom.pan((float)0.0,(float)(0.25*IGCview.width/zoom.xscale));
    paint(getGraphics());
  }
  
  void pan_south()
  {
    cancel_ruler();
    zoom.pan((float)(-0.25*IGCview.height/zoom.yscale),(float)0.0);
    paint(getGraphics());
  }
  
  void pan_west()
  {
    cancel_ruler();
    zoom.pan((float)0.0,(float)(-0.25*IGCview.width/zoom.xscale));
    paint(getGraphics());
  }

  void cancel_ruler()
    { 
      ruler = false;
    }

  void ruler_zoom()
    {
      if (ruler) cancel_ruler();
      else
        {
          ruler = true;
        }
    }

  public void clear_tracks()
    {
      cancel_ruler();
      IGCview.cursor1 = 1;
      IGCview.cursor2 = 1;
      IGCview.log_count = 0;
      IGCview.primary_index = 0;
      for (int log=1; log<=IGCview.MAXLOGS; log++) IGCview.secondary[log] = false;
      paint(getGraphics());
    }

  public void load_track()
   {
     FileDialog fd = null;
     try {
           fd = new FileDialog (IGCview.appletFrame, "Load Track Log", FileDialog.LOAD);
	   } catch (Exception e1)
                { 
                  System.out.println("IGCview: load_track(): " + e1);
                  IGCview.security_error("Cannot browse local files.");
                  return;
                }
     fd.setDirectory(IGCview.config.LOGDIR);
     fd.show ();
     String filename = fd.getFile ();
     IGCview.config.LOGDIR = fd.getDirectory();
     if (filename != null) {
       try { 
             TrackLog log =  new TrackLog(IGCview.config.LOGDIR+filename, IGCview.log_count+1, false);
             if (log.record_count==0) return;
             if (IGCview.log_count==0)
               {
                 IGCview.log_count = 1;
                 IGCview.primary_index = 1;
               }
             else
               {
                 IGCview.log_count++; 
                 IGCview.secondary[IGCview.log_count] = true;
               }
               
             IGCview.logs[IGCview.log_count] = log;
             if (IGCview.log_count==1) log.set_primary();
           } catch (Exception e) {System.out.println ("canvas.load_file " + e);}
     }
   }

  public void load_track_url(String filename)
   {
       try { 
             IGCview.log_count++;
             if (IGCview.log_count==1)
               IGCview.primary_index = 1;
             else 
               IGCview.secondary[IGCview.log_count] = true;
             TrackLog log =  new TrackLog(filename, IGCview.log_count, true);
             IGCview.logs[IGCview.log_count] = log;
             if (IGCview.log_count==1) log.set_primary();
           } catch (Exception e) {System.out.println (e);}
   }

  public void load_log_list(String log_list_name)
    {
     String buf;
     try {
          URL list_url;
          if (log_list_name.startsWith("http")) list_url=new URL(log_list_name);
          else list_url= new URL(IGCview.ApIGCview.getDocumentBase(), log_list_name);

          BufferedReader in = new BufferedReader(new InputStreamReader(list_url.openStream()));
          while ((buf = in.readLine()) != null) 
            {
              load_track_url(buf);
            }
          in.close();
          }
     catch (FileNotFoundException e) { System.err.println("load_log_list: " + e);} 
     catch (MalformedURLException e) { System.err.println("load_log_list: " + e);}
     catch (Exception e) { System.err.println("load_log_list: " + e);}
    } 

  void save_primary()
   {
      if (IGCview.primary_index==0)
        {
          IGCview.msgbox("No logs loaded.");
          return;
        }
     FileDialog fd = new FileDialog (IGCview.appletFrame, "Save track log as IGC file", FileDialog.SAVE);
     fd.setDirectory(IGCview.config.LOGDIR);
     fd.show ();
     String filename = fd.getFile ();
     IGCview.config.LOGDIR = fd.getDirectory();
     if (filename != null) {
       try { TrackLog log = IGCview.logs[IGCview.primary_index];
             log.save(IGCview.config.LOGDIR+filename);
             IGCview.msgbox("Log "+IGCview.format_date(log.date)+" "+log.name+
                          " saved in IGC format in \""+IGCview.config.LOGDIR+filename+"\".");
           } catch (Exception e) {System.out.println (e);}
     }
   }

  void select_tracks()
    {
      if (IGCview.primary_index==0)
        {
          IGCview.msgbox("No logs loaded.");
          return;
        }
      if (IGCview.select_window!=null)
        {
          IGCview.msgbox("Select tracks window already open.");
          return;
        }
      IGCview.select_window = new SelectFrame();
      IGCview.select_window.setTitle("Select Tracks");
      IGCview.select_window.pack();
      IGCview.select_window.show();
    }

  void load_tps()
   {
     try { 
           IGCview.tps.load(IGCview.config.TPFILE);
         } catch (Exception e)
            { System.err.println ("TP database load error "+e);
              IGCview.msgbox("Problem loading "+IGCview.config.TPFILE+".");
            }
   }

  //void get_task()
  //  {
  //    if (!IGCview.tps_loaded) load_tps();
  //    TaskWindow window = new TaskWindow();
  //    window.setTitle("Define Task (Using: "+IGCview.config.TPFILE+")");
  //    window.pack();
  //    window.show();
  //    window.init();  // set focus TP 1 - will this ever work????
  //  }

  void set_wind()
    {
      String speed_units = null;
      IGCview.calc_wind();
      if (IGCview.config.convert_speed==(float)1.0) speed_units = "(knots)";
      else if (IGCview.config.convert_speed==IGCview.NMKM) speed_units = "(km/h)";
      else speed_units = "(mph)";
      String s = "Wind speed: " +
		     IGCview.places(IGCview.wind_speed*IGCview.config.convert_speed,0) + speed_units + "\n" +
		     "Direction: " +
                 IGCview.places(IGCview.wind_direction,0) + " degrees\n" +
                 "Estimated using drift in "+IGCview.wind_thermal_count+" thermals.";
	    if (!IGCview.confirm(s))
	      {
 	        IGCview.wind_speed = (float) 0;
	        IGCview.wind_direction = (float) 0;
        }
    }

 void view_flight_data()
    {
      if (IGCview.primary_index==0)
        {
          IGCview.msgbox("No logs loaded.");
          return;
        }
      DataWindow.show_data();
    }

  void view_scoring()
    {
      if (IGCview.primary_index==0)
        {
          IGCview.msgbox("No logs loaded.");
          return;
        }
      if (IGCview.scoring_window!=null)
        {
          IGCview.msgbox("Scoring Data window already open.");
          return;
        }
      if (IGCview.task==null)
        {
          IGCview.msgbox("A task must be defined for Scoring Data.");
          return;
        }
      IGCview.scoring_window = new ScoringFrame();
      IGCview.scoring_window.pack();
      IGCview.scoring_window.show();
    }

  void view_tp_detail()
    {
      if (IGCview.tp_window!=null)
        { IGCview.msgbox("TP Detail window already open");
          return;
        }
      if (IGCview.task==null)
        {
          IGCview.msgbox("No task defined");
          return;
        }
      IGCview.tp_window = new TPFrame();
      //IGCview.tp_window.pack();
      IGCview.tp_window.show();
    }

  void view_config()
    {
      if (IGCview.config_window!=null)
        {
          IGCview.msgbox("Configuration window already open.");
          return;
        }
      IGCview.config_window = new ConfigFrame();
      IGCview.config_window.pack();
      IGCview.config_window.show();
    }

  public float x_to_long(int x)
    {
      return (float) x / zoom.xscale + zoom.longitude;
    } 

  public float y_to_lat(int y)
    {
      return zoom.latitude - ((float) y / zoom.yscale);
    }

  void draw_cursors(Graphics g)
    {
      if (IGCview.cursor1==0) return;
      draw_cursor1(g, IGCview.cursor1);
      if (IGCview.cursor1!=IGCview.cursor2) draw_cursor2(g, IGCview.cursor2);
    }

  void draw_cursor1(Graphics g, int i) // i is index into primary track log
    {
      int x = long_to_x(IGCview.logs[IGCview.primary_index].longitude[i]);
      int y = lat_to_y(IGCview.logs[IGCview.primary_index].latitude[i]);

      g.setColor(IGCview.config.MAPBACKGROUND.colour);
      g.setXORMode(IGCview.config.CURSORCOLOUR.colour);
      g.drawLine(x-14,y,x-4,y);
      g.drawLine(x+4,y,x+14,y);
      g.drawLine(x,y-14,x,y-4);
      g.drawLine(x,y+4,x,y+14);
      g.setPaintMode();
    }

  void draw_cursor2(Graphics g, int i) // i is index into primary track log
    {
      TrackLog log = IGCview.logs[IGCview.primary_index];
      log.cursor2 = i; // set cursor held within log - used by TrackLog.split()
      int x = long_to_x(log.longitude[i]);
      int y = lat_to_y(log.latitude[i]);

      g.setColor(IGCview.config.MAPBACKGROUND.colour);
      g.setXORMode(IGCview.config.CURSORCOLOUR.colour);
      g.drawLine(x-10,y-10,x-2,y-2);  // NW
      g.drawLine(x-10,y+10,x-2,y+2);  // SW
      g.drawLine(x+2,y-2,x+10,y-10);  // NE
      g.drawLine(x+2,y+2,x+10,y+10);  // SE
      g.setPaintMode();
    }

  void move_cursor1(int i) // move cursor 1 to index i
    {
      Graphics g = getGraphics();
      if (IGCview.cursor1!=0) draw_cursor1(g, IGCview.cursor1); // remove old cursor1
      draw_cursor1(g, i);                                       // draw new cursor1
      IGCview.cursor1 = i;

      TrackLog log = IGCview.logs[IGCview.primary_index];
      if(!zoom.inview(log.latitude[i],log.longitude[i]))
        {
	        paint(g);
        }

      CursorWindow.update();
    }

  void move_cursor2(int i)
    {
      if (i<1) return;
      Graphics g = getGraphics();
      if (IGCview.cursor2!=0) draw_cursor2(g, IGCview.cursor2); // remove old cursor2
      draw_cursor2(g, i);                                       // draw new cursor2
      IGCview.cursor2 = i;

      TrackLog log = IGCview.logs[IGCview.primary_index];
      if(!zoom.inview(log.latitude[i],log.longitude[i]))
        {
	        paint(g);
        }
      CursorWindow.update();
    }

  void maggot_synchro()
    {
      if (racing)
        {
          IGCview.msgbox("Replay already in progress - select 'Cancel' to clear.");
          return;
        }
      if (IGCview.primary_index==0)
        {
          IGCview.msgbox("No logs loaded.");
          return;
        }
	    Graphics g = getGraphics();
	    racing = true;
      paint(g);
	    maggot_racer = new MaggotRacer(g,this,true);
	    maggot_racer.start();
    }

  void maggot_real_time()
    {
      if (racing)
        {
          IGCview.msgbox("Replay already in progress - select 'Cancel' to clear.");
          return;
        }
      if (IGCview.primary_index==0)
        {
          IGCview.msgbox("No logs loaded.");
          return;
        }
	    Graphics g = getGraphics();
	    racing = true;
      paint(g);
	    maggot_racer = new MaggotRacer(g,this,false);
	    maggot_racer.start();
    }

  void maggot_cancel()
    {
	if (racing)
          { 
      	Graphics g = getGraphics();
            racing = false;
            maggot_racer.interrupt();
            paint(g);
          }
      else
        {
          IGCview.msgbox("No replay in progress to cancel.");
        }    
    }

  // RacingCanvas interface:

  public void mark_time(Graphics g, int time) // display current time on canvas during maggot race
    {
	    // Color c = IGCview.config.TPTEXT.colour;
        // g.setColor(IGCview.config.MAPBACKGROUND.colour);
        g.setColor(Color.yellow);
        g.setPaintMode();
        g.fillRect(65,3,60,15);
        g.setColor(Color.black);
	    g.drawString(IGCview.format_clock(time), 70, 14);
    }

  public void mark_plane(Graphics g, int log_num, int x, int y, int h) // draw plane at x,y
                                                                // with stick length h
    {
	    Color c = IGCview.config.TRACKCOLOUR[IGCview.config.FCOLOUR[log_num][0]];
        g.setColor(IGCview.config.MAPBACKGROUND.colour);
        g.setXORMode(c);
        if (IGCview.config.altsticks)
          {
            if (IGCview.config.upside_down_lollipops)
              {
                g.drawLine(x,y-4-h,x,y-4);
              }
            else
              {
                g.drawLine(x,y-h,x,y);
                g.fillOval(x-4,y-h-8,8,8);
                g.setColor(c);
                g.setXORMode(IGCview.config.TRACKCOLOUR[IGCview.config.FCOLOUR[log_num][1]]);
                g.fillOval(x-2,y-h-6,4,4);
                return;
              }
          }
        g.fillOval(x-4,y-4,8,8);
        g.setColor(c);
        g.setXORMode(IGCview.config.TRACKCOLOUR[IGCview.config.FCOLOUR[log_num][1]]);
        g.fillOval(x-2,y-2,4,4);
    }

  public void mark_icon(Graphics g, int log_num, int x, int y, int h, float track) // draw plane at x,y
                                                                // with stick length h, on track 
    {
        int x1,y1,x2,y2;
        double track_radians = track / 180 * Math.PI;
        double cos_track = Math.cos(track_radians);
        double sin_track = Math.sin(track_radians);

	Color c = IGCview.config.TRACKCOLOUR[IGCview.config.FCOLOUR[log_num][0]];
        g.setColor(IGCview.config.MAPBACKGROUND.colour);
        g.setXORMode(c);
        if (IGCview.config.altsticks)
          {
            g.drawLine(x,y-h,x,y);
            if (!IGCview.config.upside_down_lollipops)
              {
                x1 = x+(int)(cos_track * (-10) - sin_track * (-1));
                y1 = y-h+(int)(sin_track * (-10) + cos_track * (-1));
                x2 = x+(int)(cos_track * 10 - sin_track * (-1));
                y2 = y-h+(int)(sin_track * 10 + cos_track * (-1));
                g.drawLine(x1,y1,x2,y2);
                x1 = x+(int)(cos_track * (-2) - sin_track * 6);
                y1 = y-h+(int)(sin_track * (-2) + cos_track * 6);
                x2 = x+(int)(cos_track * 2 - sin_track * 6);
                y2 = y-h+(int)(sin_track * 2 + cos_track * 6);
                g.drawLine(x1,y1,x2,y2);
                g.setXORMode(IGCview.config.TRACKCOLOUR[IGCview.config.FCOLOUR[log_num][1]]);
                x1 = x+(int)(cos_track * 0 - sin_track * (-3));
                y1 = y-h+(int)(sin_track * 0 + cos_track * (-3));
                x2 = x+(int)(cos_track * 0 - sin_track * 6);
                y2 = y-h+(int)(sin_track * 0 + cos_track * 6);
                g.drawLine(x1,y1,x2,y2);
                return;
              }
          }
        x1 = x+(int)(cos_track * (-10) - sin_track * (-1));
        y1 = y+(int)(sin_track * (-10) + cos_track * (-1));
        x2 = x+(int)(cos_track * 10 - sin_track * (-1));
        y2 = y+(int)(sin_track * 10 + cos_track * (-1));
        g.drawLine(x1,y1,x2,y2);
        x1 = x+(int)(cos_track * (-2) - sin_track * 6);
        y1 = y+(int)(sin_track * (-2) + cos_track * 6);
        x2 = x+(int)(cos_track * 2 - sin_track * 6);
        y2 = y+(int)(sin_track * 2 + cos_track * 6);
        g.drawLine(x1,y1,x2,y2);
        g.setXORMode(IGCview.config.TRACKCOLOUR[IGCview.config.FCOLOUR[log_num][1]]);
        x1 = x+(int)(cos_track * 0 - sin_track * (-3));
        y1 = y+(int)(sin_track * 0 + cos_track * (-3));
        x2 = x+(int)(cos_track * 0 - sin_track * 6);
        y2 = y+(int)(sin_track * 0 + cos_track * 6);
        g.drawLine(x1,y1,x2,y2);
    }

  public void draw_plane(Graphics g, int log_num, int i) // draw plane at log index i
    { 
      TrackLog log = IGCview.logs[log_num];
      float glider_lat = log.latitude[i];
      float glider_long = log.longitude[i];
      int x = long_to_x(glider_long);
      int y = lat_to_y(glider_lat);
      int h = (int)(log.altitude[i] / 200);
      if (IGCview.config.glider_icons)
        { 
          float track = (i==1) ? 0 : IGCview.dec_to_track(log.latitude[i-1], log.longitude[i-1],
                                                          glider_lat, glider_long);
          mark_icon(g, log_num, x, y, h, track);
        }
      else
        mark_plane(g, log_num, x, y, h);
    }

  public void draw_legend(Graphics g)
    {
      int legend_y = 10;
      TrackLog log;
      for (int i=1; i<=IGCview.log_count; i++)
	      {
	        if (i==IGCview.primary_index || IGCview.secondary[i])
	          {
	    	      log = IGCview.logs[i];
		          if (IGCview.config.glider_icons)
                mark_icon(g,i,12,legend_y,0,(float)0);
              else
                mark_plane(g,i,12,legend_y,0);
		          g.setColor(Color.black);
		          g.setPaintMode();
		          g.drawString(log.name, 25, legend_y+4);
		          legend_y += 12;
	          }
	      }
    }

  // KeyListener interface:

  public void keyPressed(KeyEvent e)
    {
      if (racing)
        racing_key(e.getKeyCode());
      else
        map_key(e.getKeyCode());
    }

  public void keyReleased(KeyEvent e) {;}

  public void keyTyped(KeyEvent e) {;}

  void racing_key(int key)
    {
      switch (key)
        {
	        case KeyEvent.VK_RIGHT: 
            race_forwards();
            break;
	        case KeyEvent.VK_LEFT:
            race_backwards();
            break;
          case KeyEvent.VK_PAUSE:
            race_pause();
            break;
          case KeyEvent.VK_UP:
            race_faster();
            break;
          case KeyEvent.VK_DOWN:
            race_slower();
            break;
          default:
        }
    }

  void map_key(int key)
    {
      if (IGCview.primary_index==0) return;
      switch (key)
        {
	        case KeyEvent.VK_SPACE: 
	          //System.out.println("space: move forward!");
	          map_cursor_right();
	          break;
	        case KeyEvent.VK_BACK_SPACE:
	          //System.out.println("backspace: move back!");
	          map_cursor_left();
	          break;
	        case KeyEvent.VK_INSERT:
	          //System.out.println("inser: move!");
	          map_insert();
	          break;
	        case KeyEvent.VK_HOME:
	          //System.out.println("home: move home!");
	          zoom_to_home();
	        case KeyEvent.VK_S:
	          //System.out.println("S: move to start!");
	          map_home();
	          break;
	        case KeyEvent.VK_E:
            //System.out.println("E: move to end");
	          map_end();
	          break;
	        case KeyEvent.VK_PAGE_UP:
	          //System.out.println("page up: zoom out!");
	          zoom_out();
	          break;
	        case KeyEvent.VK_PAGE_DOWN:
	          //System.out.println("page down: zoom in!");
	          zoom_in();
	          break;
	        case KeyEvent.VK_UP:
	          //System.out.println("up: North!");
	          pan_north();
	          break;
	        case KeyEvent.VK_DOWN:
	          //System.out.println("down: South!");
	          pan_south();
	          break;
	        case KeyEvent.VK_LEFT:
	          //System.out.println("left: West!");
	          pan_west();
	          break;
	        case KeyEvent.VK_RIGHT:
	          //System.out.println("right: East!");
	          pan_east();
	          break;
	        default:
        }
    }

  void map_cursor_right()
    {
      TrackLog log = IGCview.logs[IGCview.primary_index];
      int i = IGCview.cursor2;
      if (log.gps_ok)
        while (i<log.record_count &&
               log.latitude[i++]==(float)0.0) {;} // skip bad recs
      move_cursor2(i);
    }

  void map_cursor_left()
    {
      TrackLog log = IGCview.logs[IGCview.primary_index];
      int i = IGCview.cursor2;
      if (log.gps_ok)
        while (i>1 &&
               log.latitude[i--]==(float)0.0) {;} // skip bad recs
      move_cursor2(i);
    }

  void map_insert()
    {
      move_cursor1(IGCview.cursor2);
      map_cursor_right();
    }

  void map_home()
    {
      TrackLog log = IGCview.logs[IGCview.primary_index];
      int i = 1;
      if (log.gps_ok)
        while (i<log.record_count &&
               log.latitude[i++]==(float)0.0) {;} // skip bad recs
      move_cursor1(i);
    }

  void map_end()
    {
      TrackLog log = IGCview.logs[IGCview.primary_index];
      int i = log.record_count;
      if (log.gps_ok)
        while (i>1 &&
               log.latitude[i--]==(float)0.0) {;} // skip bad recs
      move_cursor2(i);
    }

  void race_forwards()
    {
      if (maggot_racer!=null)
        {
          maggot_racer.race_forwards();
        }
    }

  void race_backwards()
    {
      if (maggot_racer!=null)
        {
          maggot_racer.race_backwards();
        }
    }

  void race_pause()
    {
      if (maggot_racer!=null)
        maggot_racer.race_pause();
    }

  void race_faster()
    {
      if (maggot_racer!=null)
        maggot_racer.race_faster();
    }

  void race_slower()
    {
      if (maggot_racer!=null)
        maggot_racer.race_slower();
    }

}

//***************************************************************************//
//            TrackLog                                                       //
//***************************************************************************//

class TrackLog
{
  int log_index=0;	// index of this log. This value will be set such that 
				            // this Tracklog = IGCview.logs[log_index]
  boolean baro_ok=false,
          gps_ok=false,
          gps_alt_ok=false; // these flags set to 'true' on good latitude and altitude 'B' records
          
  String date = null; // date from HFDTE record
  String name;            // name from HFGID, HFCID records or file name
  
  // here are the arrays of data from the 'B' records, one set of elements per fix
  // the 'B' record can contain both GPS and Baro altitude.  IGCview stores two altitude arrays, the
  // primary (altitude[]) and alternate (alt_altitude[]).  Initially, baro alt is loaded into the 'altitude[]'
  // array, and GPS alt into the 'alt_altitude[]' array, and the 'gps_altitude' boolean is set to false.  If
  // the user selects 'GPS altitude', then the contents of the arrays will be switched, and the 'gps_altitude'
  // flag will be set to 'true'
  int   [] time      = new int [IGCview.MAXLOG+1];    // Time of day in seconds, alt in feet
  float [] altitude  = new float [IGCview.MAXLOG+1];     // altitude (initially barometric) from 'B' records
  float [] alt_altitude  = new float [IGCview.MAXLOG+1]; // alternate altitude (initially GPS) from 'B' records
  float [] latitude  = new float [IGCview.MAXLOG+1]; 
  float [] longitude = new float [IGCview.MAXLOG+1];  // W/S = negative, N/E = positive
  
  static final int GPS_ALT = 0, BARO_ALT = 1; // constants for GPS and BAROMETRIC altitude
  
  int altitude_type = BARO_ALT; // flag to specify whether gps (GPS_ALT) or baro (BARO_ALT) altitude is current
                                // if 'altitude_type' is 'GPS_ALT' then 'altitude[]' array contains GPS altitude
                                // otherwise it will contain Baro altitude
  Task task = new Task();              // task from C records
  int   record_count = 0;                              // number of records in log

  Climb [] thermal = new Climb [IGCview.MAXTHERMALS+1];
  int  thermal_count = 0;                              // number of thermals in log
 
  float [] climb_percent = new float [IGCview.CLIMBBOXES+1];
  float climb_avg = (float) 0.0;
  float task_climb_avg = (float) 0.0;
  float task_climb_height = (float) 0.0;
  int   task_climb_time = 0;
  float task_climb_percent = (float) 0.0;
  int   task_climb_count = 0;
  int   task_time = 0;
  float task_distance = (float) 0.0; // distance achieved around task (in nm)
  float task_speed = (float) 0.0;
  float task_cruise_avg = (float) 0.0;

  int   [] tp_index  = new int [IGCview.MAXTPS+1];     // index of log point at each TP
  int landed_index = 1;                                // index of log point on landout
  int   [] tp_time  = new int [IGCview.MAXTPS+1];      // time rounded each TP
  float [] tp_altitude = new float [IGCview.MAXTPS+1]; // height rounded each TP (ft)
  
  // tp_latitude[tp_number] and tp_longitude[tp_number] are coordinates of turn
  //   if tp is an area turnpoint then these coordinates will be from a selected track point
  //   otherwise these coordinates will be the lat,long of the actual TP
  float [] tp_latitude = new float [IGCview.MAXTPS+1];
  float [] tp_longitude = new float [IGCview.MAXTPS+1];
  
  // these are the calculated performance stats for each leg 
  int   [] leg_time = new int [IGCview.MAXTPS];        // duration for each leg (s)
  float [] leg_speed = new float [IGCview.MAXTPS];     // speed for each leg (knots)
  float [] leg_climb_avg = new float [IGCview.MAXTPS];      // average climb for each leg (knots)
  float [] leg_climb_percent = new float [IGCview.MAXTPS];  // percent time climbing for each leg
  int   [] leg_climb_count = new int [IGCview.MAXTPS];      // percent time climbing for each leg
  float [] leg_cruise_avg = new float [IGCview.MAXTPS];     // average cruise speed for each leg
  float [] leg_distance = new float [IGCview.MAXTPS];  // achieved length of each actual leg. Differs from task
                                                       // leg distances when area tp's are in the task
  int tps_rounded = 0;                                 // TPs successfully rounded

  int cursor2 = 0; // logpoint index of latest cursor2 setting from click on canvas - used by split()

  // TrackLog constructor:

  TrackLog() {;} // constructor for empty TrackLog used by 'split()'
  
  // usual constructor for new TrackLog built from IGC or gardown file
  TrackLog (String filename, int log_index, boolean is_url)
  {
    // note: this procedure reads records from the url or file 'filename', and adds
    //   tracklog records as they are read, updating the 'date' and 'name' fields
    //   as appropriate records are found.  Only one procedure is needed for both
    //   gardown and .igc files, as the tracklog records "B..." and "T ..." are
    //   distinct in each file type.
    int i,j;
    BufferedReader in = null;
    boolean is_igc=false, is_gardown=false; // flags set when filetype is recognised
    int c_record_count = 0;  // skip the first 2 C records (preamble,takeoff)
                             // before using C records to build task.
                             // Also skip last C record (landing).
    TP last_tp = new TP();  // saves previous TP found in 'C' record, as we don't want the final one

    this.log_index = log_index;
    try { 
          IGCview.show_status("Loading file '"+filename+"'...");
	        if (is_url)
            {
	            URL log_url;
	            if(filename.startsWith("http")) log_url=new URL(filename);
	            else log_url= new URL(IGCview.ApIGCview.getDocumentBase(), filename);

		          in = new BufferedReader( new InputStreamReader(log_url.openStream()));
	    	    }
	        else in = new BufferedReader(new FileReader(filename));
          String buf;
          int day_offset=0;
          time[0]=0;
          while ((buf = in.readLine()) != null) 
          { 
            if (buf.length()==0) continue;
            if (!is_gardown && buf.charAt(0) == 'B')
              { 
                // process 'B' records, the time/lat/long/alt records in the file
                // ignore all characters beyond the 35th
                if (buf.length()<35) continue; // skip record if not long enough
                is_igc = true;
                record_count++;
                if (record_count==IGCview.MAXLOG)
                  {
                    IGCview.msgbox("Too many records in "+filename+" (limit="+IGCview.MAXLOG+")");
                    break;
                  }
                latitude[record_count] = IGCview.decimal_latlong(buf.substring(7,9),
                                                         buf.substring(9,11)+"."+buf.substring(11,14),
                                                         buf.charAt(14));
                if (latitude[record_count]!=(float)0.0 && buf.charAt(24)=='V')
                  {
                    record_count--; // skip really bad B records (e.g. nz.igc)
                    continue;
                  }
                longitude[record_count] = IGCview.decimal_latlong(buf.substring(15,18),
                                                          buf.substring(18,20)+"."+buf.substring(20,23),
                                                          buf.charAt(23));
                time[record_count] = Integer.valueOf(buf.substring(1,3)).intValue()*3600 +
                                     Integer.valueOf(buf.substring(3,5)).intValue()*60 + 
                                     Integer.valueOf(buf.substring(5,7)).intValue() + day_offset;
                if (time[record_count]<time[record_count-1])
                  {
                    day_offset += 86400; // if time seems to have gone backwards, add a day
                    time[record_count] += 86400;
                  }
                altitude[record_count] = Float.valueOf(buf.substring(25,30)).floatValue() *
                                           (float) 3.2808 ; // Baro altitude
                alt_altitude[record_count] = Float.valueOf(buf.substring(30,35)).floatValue() *
                                           (float) 3.2808 ; // GPS altitude
                if (latitude[record_count]!=(float)0.0) gps_ok=true;
                if (altitude[record_count]!=(float)0.0) baro_ok=true;
                if (alt_altitude[record_count]!=(float)0.0) gps_alt_ok=true;
	        }
            if (!is_gardown && buf.charAt(0) == 'C' && c_record_count<=IGCview.MAXTPS+3)
              { 
                is_igc = true;
                c_record_count++;
                if (c_record_count>IGCview.MAXTPS+3)
                  {
                    IGCview.msgbox("Didn't get task from log. (>"+
                                          IGCview.MAXTPS+
                                          " 'C' TP records in IGC log file)");
                    task.tp_count = 0; // invalidate task
                    continue;
                  }
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
                altitude[record_count] = (float) 0.0;
                if (latitude[record_count]!=(float)0.0) gps_ok=true;
                if (altitude[record_count]!=(float)0.0) baro_ok=true; // for future baro gps
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
        } catch (FileNotFoundException e) {
	          IGCview.msgbox("File not found: "+filename);
            System.err.println("TrackLog: " + e);
          } catch (Exception e) {
	            IGCview.msgbox("IGCview error: loading file: "+filename+"\n"+e);
              System.err.println("TrackLog: " + e);
            }
      IGCview.show_status("");

    }
  
  // split TrackLog at current cursor point into two TrackLogs 
  void split()
    {
      int i, j=1; // record counters in this log (i) and new log (j)
      
      int split_index = cursor2; // index of TrackLog to start copy into new log
      if (split_index==0) split_index = 1;
      
      TrackLog new_log = new TrackLog(); // create new blank TrackLog
      new_log.name = name + "_2";        // set name of new TrackLog "xxx_2"
      name = name + "_1";                // set name of current TrackLog "xxx_1"
      new_log.date = date;               // set new TrackLog to same date
      new_log.task = task;               // set new TrackLog to same task
      new_log.baro_ok = baro_ok;         // set new TrackLog to confirm alt readings ok
      new_log.gps_ok = gps_ok;           // set new TrackLog to confirm gps readings ok
      for (i = split_index; i<=record_count; i++)  // copy alt,lat,long,time records
        {
          new_log.time[j] = time[i];
          new_log.altitude[j] = altitude[i];
          new_log.latitude[j] = latitude[i];
          new_log.longitude[j] = longitude[i];
          j++;
        }
      new_log.record_count = j - 1;      // set new TrackLog record_count
      if (split_index>1) record_count = split_index; // trim this record count at split
                                                     // if split_index at beginning then clone log 
      IGCview.log_count++;
      IGCview.logs[IGCview.log_count] = new_log;
      new_log.log_index = IGCview.log_count;
      IGCview.secondary[new_log.log_index] = true;
      new_log.calc_flight_data();        // initialize new TrackLog tp/thermal indexes
      calc_flight_data();                // re calculate thermals & tp time for trimmed log
      IGCview.msgbox("Primary log split into " + name + " and " + new_log.name);
    }
    
  boolean is_primary() 
    {
      return (log_index==IGCview.primary_index);
    }

  // switch between altitude[] and alt_altitude
  void set_altitude(int alt_type)
    {
      float temp;
      if (alt_type==altitude_type) return;
      for (int i=1; i<=record_count; i++)
        {
          temp = altitude[i];
          altitude[i] = alt_altitude[i];
          alt_altitude[i] = temp;
        }
      altitude_type = alt_type;
    }
    
  void save(String filename)
    {
      try { BufferedWriter out = new BufferedWriter(new FileWriter(filename));
            out.write("AEWAB1529 9716"); // write manufacturer record - copied from my EW
            out.newLine();
            out.write("HFDTE"+date);
            out.newLine();
            out.write("HFGIDGlider ID: "+name);
            out.newLine();
            if (IGCview.task!=null)
              {
                out.write("C"+date+"000000"+date+"0001"+IGCview.two_digits(IGCview.task.tp_count-1)); // write task header
                out.newLine();
                out.write(tp_to_igc_line(IGCview.task.tp[1])); // write take-off record
                out.newLine();
                for (int i=1; i<=IGCview.task.tp_count; i++)
		  {
                    out.write(tp_to_igc_line(IGCview.task.tp[i])); // write task TP records
                    out.newLine();
                  }
                out.write(tp_to_igc_line(IGCview.task.tp[IGCview.task.tp_count])); // write landing record
                out.newLine();
              }
            for (int i=1; i<=record_count; i++)
              { 
                out.write(log_index_to_igc_line(i));
                out.newLine();
              }
            out.close();
          } catch (IOException e) {
              System.err.println("TrackLog save: " + e);
	  }
    }

  String tp_to_igc_line(TP tp)
    {
      return "C"+lat_to_igc(tp.latitude)+long_to_igc(tp.longitude)+tp.trigraph;
    }

  static String lat_to_igc(float latitude) // convert latitude into DDMMmmm (= DD degrees, MM.mmm minutes)
    {
      String NS = (latitude<0 ? "S" : "N");
      float abs_latitude = Math.abs(latitude);
      int degrees = (int) abs_latitude;
      int minutes = (int) ((abs_latitude - degrees)*60000);
      return IGCview.two_digits(degrees)+IGCview.five_digits(minutes)+NS;
    }

  static String long_to_igc(float longitude) // convert longitude into DDDMMmmm (= DDD degrees, MM.mmm minutes)
    {
      String EW = (longitude<0 ? "W" : "E");
      float abs_longitude = Math.abs(longitude);
      int degrees = (int) abs_longitude;
      int minutes = (int) ((abs_longitude - degrees)*60000);
      return IGCview.three_digits(degrees)+IGCview.five_digits(minutes)+EW;
    }

  String time_to_igc(int time)
    {
      int h = time / 3600;
      int m = (time-(h*3600)) / 60;
      int s = time - (h*3600) - (m*60);
      h = h % 86400;
      String hh = IGCview.two_digits(h);
      String mm = IGCview.two_digits(m);
      String ss = IGCview.two_digits(s);
      return hh+mm+ss;
    }

  String log_index_to_igc_line(int i)
    {
      return "B"+time_to_igc(time[i])+lat_to_igc(latitude[i])+long_to_igc(longitude[i])+
                   (latitude[i]==(float)0.0 ? "V" : "A")+alt_to_igc(altitude[i])+"00000000";
    }

  String alt_to_igc(float altitude)
    {
      return IGCview.five_digits((int) (altitude*IGCview.FTM));
    }

  void set_primary()
    {
       IGCview.canvas.set_title();
       if (task.tp_count>1 && !task.equals(IGCview.task))
         {
           if (IGCview.task==null || IGCview.confirm("Primary log contains new task - do you want to use it?"))
             IGCview.new_task(task);
         }
	     if (IGCview.config.auto_zoom) IGCview.canvas.zoom.zoom_to_home();
    }

  public void draw(Drawable map, Graphics g, int num)
  {
    int x1,x2,y1,y2, i, max_i;

    if (!gps_ok) return;
    g.setColor(IGCview.config.TRACKCOLOUR[num % IGCview.config.MAXCOLOURS]);

    i = (IGCview.config.trim_logs && tps_rounded>0) ? tp_index[1]-5 : 0;
    while (latitude[++i]==(float)0.0) {;} // skip bad gps records
    x1 = map.long_to_x(longitude[i]);
    y1 = map.lat_to_y(latitude[i]);
    max_i = (IGCview.config.trim_logs && 
             IGCview.task!=null &&
             tps_rounded==IGCview.task.tp_count) ? tp_index[tps_rounded]+2 : record_count;
    boolean thicken = IGCview.ApIGCview.draw_images_mode && is_primary(); // thicken line if map image underneath
    while (++i < max_i)
    {
        if (latitude[i]==(float)0.0) continue;
        x2 = map.long_to_x(longitude[i]);
        y2 = map.lat_to_y(latitude[i]);
        g.drawLine(x1,y1,x2,y2);
        if (thicken) // thick line if primary & map underneath
          {
            g.drawLine(x1+1,y1,x2+1,y2);
            g.drawLine(x1,y1+1,x2,y2+1);
          }
        x1 = x2;
        y1 = y2;
    }
  }

  public void draw_alt(Graphable canvas, Graphics g, boolean primary)
    {
      if (primary) draw_alt_tp_lines(canvas, g);           // Draw TP lines
      if (!baro_ok) return;
      g.setColor(primary ? IGCview.config.ALTCOLOUR.colour : IGCview.config.ALTSECCOLOUR.colour);
      if (canvas.x_axis()==IGCview.TIMEX)                // Draw alt trace
        draw_time_alt(canvas, g);
      else
        draw_dist_alt(canvas, g);
    }

  private void draw_time_alt(Graphable canvas, Graphics g)
    {
      int x1,x2,y1,y2;

      x1 = canvas.time_to_x(time[1]);
      y1 = canvas.get_y(altitude[1]);
      for (int i = 2; i <= record_count; i++)
        {
          x2 = canvas.time_to_x(time[i]);
          y2 = canvas.get_y(altitude[i]);
          g.drawLine(x1,y1,x2,y2);
          x1 = x2;
          y1 = y2;
        }
    }

  private void draw_dist_alt(Graphable canvas, Graphics g)
    {
      int x1,x2,y1,y2, next_tp=1;
      float dist, tp_lat, tp_long, tp_dist = (float) 0.0;

      if (tps_rounded==0) return;
      tp_lat = IGCview.task.tp[1].latitude;
      tp_long = IGCview.task.tp[1].longitude;
      x1 = canvas.dist_to_x(0);
      y1 = canvas.get_y(altitude[1]);
      for (int i = 1; i <= record_count; i++)
        {
          if (latitude[i]==(float)0.0) continue;
          x2 = canvas.dist_to_x(index_to_dist(i));
          y2 = canvas.get_y(altitude[i]);
          g.drawLine(x1,y1,x2,y2);
          x1 = x2;
          y1 = y2;
        }
    }

  void draw_alt_tp_lines(Graphable canvas, Graphics g)
    {
      int x, h = canvas.get_y((float) 0), tp_max;
      float tp_dist = (float) 0;

      g.setColor(IGCview.config.TPBARCOLOUR.colour);
      if (IGCview.task==null || tps_rounded==0) return;                     // Draw TP lines
      tp_max = (canvas.x_axis()==IGCview.TIMEX) ? tps_rounded : IGCview.task.tp_count;
      for (int tp=1; tp<=tp_max; tp++)
        {
          if (canvas.x_axis()==IGCview.TIMEX)
              x = canvas.time_to_x(time[tp_index[tp]]);
          else
            {
              x = canvas.dist_to_x(tp_dist);
              tp_dist += leg_distance[tp];
            }
          g.drawLine(x,0,x,h);
          x++;
          g.drawLine(x,0,x,h);
          x += 3;
          g.drawString(IGCview.task.tp[tp].trigraph,x,11);
        }
    }

  public void draw_climb(Graphable canvas, Graphics g, boolean primary)
    {
      find_thermals();
      if (primary) draw_alt_tp_lines(canvas, g);           // Draw TP lines
      g.setColor(primary ? IGCview.config.CLIMBCOLOUR.colour : IGCview.config.CLIMBSECCOLOUR.colour);
      if (canvas.x_axis()==IGCview.TIMEX)                // Draw alt trace
        draw_time_climb(canvas, g, primary);
      else
        draw_dist_climb(canvas, g, primary);
    }

  public void draw_time_climb(Graphable canvas, Graphics g, boolean primary)
    {
      int x,y,w,h,i;

      h = primary ? 5 : 4;
      for (i=1; i<=thermal_count; i++)
        {
          if (tps_rounded>0)
            if (thermal[i].finish_index < tp_index[1]) continue;  // not yet started task
            else if (tps_rounded==IGCview.task.tp_count &&        // completed task
                     thermal[i].finish_index > tp_index[tps_rounded]) break;
          x = canvas.time_to_x(time[thermal[i].start_index]);
          w = canvas.time_to_x(time[thermal[i].finish_index]) - x;
          y = canvas.get_y(thermal[i].rate())-2;
          g.fillRect(x,y,w,h);
        }
    }

  public void draw_dist_climb(Graphable canvas, Graphics g, boolean primary)
    {
      int x,y,w,h,i,next_tp=1;
      float dist, tp_lat, tp_long, tp_dist = (float) 0.0;

      if (tps_rounded==0) return;
      h = primary ? 5 : 4;
      tp_lat = IGCview.task.tp[1].latitude;
      tp_long = IGCview.task.tp[1].longitude;
      for (i=1; i<=thermal_count; i++)
        {
          if (tps_rounded>0)
            if (thermal[i].finish_index < tp_index[1]) continue;  // not yet started task
            else if (tps_rounded==IGCview.task.tp_count &&        // completed task
                     thermal[i].finish_index > tp_index[tps_rounded]) break;
          x = canvas.dist_to_x(index_to_dist(thermal[i].start_index));
          y = canvas.get_y(thermal[i].rate())-2;
          w = canvas.dist_to_x(index_to_dist(thermal[i].finish_index)) - x;
          if (w<0)  // some corrections as you can move backwards while thermalling
            w=-w;
	        if (w<3) // if scale such that thermal would diaply very narrow on distance axis, min w = 3
            w=3;
          g.fillRect(x,y,w,h);
        }
    }

  public void draw_cruise(Graphable canvas, Graphics g, boolean primary)
    {
      if (!gps_ok) return;
      find_thermals();
      if (primary) draw_alt_tp_lines(canvas, g);           // Draw TP lines
      g.setColor(primary ? IGCview.config.CRUISECOLOUR.colour : IGCview.config.CRUISESECCOLOUR.colour);
      if (canvas.x_axis()==IGCview.TIMEX)                // Draw alt trace
        draw_time_cruise(canvas, g, primary);
      else
        draw_dist_cruise(canvas, g, primary);
    }

  public void draw_time_cruise(Graphable canvas, Graphics g, boolean primary)
    {
      float dist;
      int x,y,w,h,i, i1, i2;

      h = primary ? 5 : 4;
      for (i=1; i<=thermal_count; i++)
        {
          i1 = thermal[i].finish_index;
          i2 = (i<thermal_count) ? thermal[i+1].start_index : record_count; 
          if (tps_rounded>0)
            if (thermal[i].finish_index < tp_index[1]) continue;  // not yet started task
            else if (tps_rounded==IGCview.task.tp_count)          // completed task
                   if (i1 > tp_index[tps_rounded]) break;
                   else if (i2 > tp_index[tps_rounded]) i2 = tp_index[tps_rounded];
          dist = IGCview.dec_to_dist(latitude[i1], longitude[i1],
                                     latitude[i2], longitude[i2]);
          x = canvas.time_to_x(time[i1]);
          w = canvas.time_to_x(time[i2]) - x;
          y = canvas.get_y( dist / (time[i2]-time[i1]) * 3600)-2;
          g.fillRect(x,y,w,h);
        } 
    }

  public void draw_dist_cruise(Graphable canvas, Graphics g, boolean primary)
    {
      int x,y,w,h,i, i1, i2, next_tp=1;
      float dist, tp_lat, tp_long, tp_dist = (float) 0.0;

      if (tps_rounded==0) return;

      h = primary ? 5 : 4;
      tp_lat = IGCview.task.tp[1].latitude;
      tp_long = IGCview.task.tp[1].longitude;
      for (i=1; i<=thermal_count; i++)
        {
          i1 = thermal[i].finish_index;
          i2 = (i<thermal_count) ? thermal[i+1].start_index : record_count; 
          if (tps_rounded>0)
            if (i1 < tp_index[1]) continue;  // not yet started task
            else if (tps_rounded==IGCview.task.tp_count)          // completed task
                   if (i1 > tp_index[tps_rounded]) break;
                   else if (i2 > tp_index[tps_rounded]) i2 = tp_index[tps_rounded];
          x = canvas.dist_to_x(index_to_dist(i1));
          w = canvas.dist_to_x(index_to_dist(i2)) - x;
          if (w<0)  // maybe he's flying AWAY from the TP
            w=-w;
          else if (w==0) // or somehow in a turn
            w=1;
          dist = IGCview.dec_to_dist(latitude[i1], longitude[i1], // find distance travelled on this cruise segment
                                     latitude[i2], longitude[i2]);
          y = canvas.get_y( dist / (time[i2]-time[i1]) * 3600)-2; // find speed for this cruise segment
          g.fillRect(x,y,w,h);
        } 
    }

  public void draw_climb_profile(Graphable cp_canvas, Graphics g, boolean primary)
    {
      int x,y, i, box;
      Polygon p = new Polygon();
      Color c = primary ? IGCview.config.TRACKCOLOUR[IGCview.config.PRIMARYCOLOUR] : 
                          IGCview.config.SECTRACKCOLOUR.colour;

      if (!baro_ok) return;
      find_climb_profile();
      x = cp_canvas.climb_to_x((float) 0);
      y = cp_canvas.get_y((float) 0);
      p.addPoint(x,y);
      y = cp_canvas.get_y(climb_percent[1]);
      p.addPoint(x,y);
      for (box=1; box<=IGCview.CLIMBBOXES; box++)
        { 
          x = cp_canvas.box_to_x(box);
          y = cp_canvas.get_y(climb_percent[box]);
          p.addPoint(x,y);
        }
      x = cp_canvas.box_to_x(IGCview.CLIMBBOXES);
      y = cp_canvas.get_y((float) 0);
      p.addPoint(x,y);
      x = cp_canvas.climb_to_x(climb_avg);
      g.setColor(c);                               // Draw graph
      g.drawPolygon(p);
      if (primary)                                 // draw average climb line
        {
          g.drawLine(x,0,x,cp_canvas.get_y((float) 0));
          g.drawLine(x+1,0,x+1,cp_canvas.get_y((float) 0));
        }
    }

  void set_font(Graphics g) // set font for thermal strength
    {
      // Font font = g.getFont();
      // int style = font.getStyle();
      g.setFont(new Font("Helvetica", Font.PLAIN, 14));
    }

  public void draw_thermals(Drawable map, Graphics g)
  {
    int i,x,y,w,h;

    if (!gps_ok) return;
    if (is_primary())
      {
        if (!IGCview.config.draw_pri_thermals) return;
      }
    else if (!IGCview.config.draw_sec_thermals) return;

    if (is_primary()) 
      g.setColor(Color.green);            // draw big box around thermal boundary
    else 
      g.setColor(Color.yellow);
    set_font(g); // set font for thermal strength text
    for (i=1; i<=thermal_count; i++)
      {
        if (tps_rounded>0 && thermal[i].finish_index < tp_index[1]) continue;
        x = map.long_to_x(thermal[i].long1);
        y = map.lat_to_y(thermal[i].lat2);
        w = map.long_to_x(thermal[i].long2) - x;
        h = map.lat_to_y(thermal[i].lat1) - y;
        g.drawRect(x,y,w,h);					    // draw box around thermal
	      if (IGCview.config.draw_pri_thermal_strengths)    // write text thermal strength above box
	        { 
            g.drawString(IGCview.places(thermal[i].rate()*IGCview.config.convert_climb,1), x, y-2);
          }
      }
    if (IGCview.config.draw_pri_thermal_ends)          // mark thermal entry and exit points
      for (i=1; i<=thermal_count; i++)
        { 
          if (tps_rounded>0 && thermal[i].finish_index < tp_index[1]) continue;
          mark_point(map, g, Color.green, thermal[i].start_index);
          mark_point(map, g, Color.red, thermal[i].finish_index);
        }
  }

  void draw_tp_times(Drawable map, Graphics g)
    {
      if (!IGCview.config.draw_tp_times || !gps_ok) return;
      for (int i = 1; i <= tps_rounded; i++) mark_point(map, g, Color.yellow, tp_index[i]);
      if (IGCview.task!=null && 
          tps_rounded<IGCview.task.tp_count)
        mark_point(map, g, Color.yellow, landed_index);
    }

  void mark_point(Drawable map, Graphics g, Color c, int index)
    { 
      int x, y, w ,h;
      g.setColor(c);
      x = map.long_to_x(longitude[index]) - 3;
      y = map.lat_to_y(latitude[index]) - 3;
      w = 6;
      h = 6;
      g.drawRect(x,y,w,h);
    }

  void find_thermals()
    {
      int i = 0, i_max, j;
      float lat1, long1, lat2, long2; // boundaries of thermal

      if (!gps_ok || thermal_count != 0) return;
      i_max = record_count;
      if (tps_rounded > 0)
         { i = tp_index[1] - 1; // if TP times found then use start time
           if (tps_rounded == IGCview.task.tp_count)
              i_max = tp_index[tps_rounded]; // if all TPs rounded use finish time
         }
      while (++i < i_max && thermal_count < IGCview.MAXTHERMALS)
        {
	  if (latitude[i]==(float)0.0) continue; // skip bad gps records
          if (thermalling(i))
            {
              j = i;
              while (++j<record_count && (latitude[j]==(float)0.0 || thermalling(j))) {;} // scan to end of thermal
                                           // skip last "thermal"
              if (time[record_count]-time[j] <= IGCview.LASTTHERMALLIMIT) break;
              lat1 = latitude[j];
              long1 = longitude[j];
              lat2 = lat1;
              long2 = long1;
              for (int k=i; k<j; k++)     // accumulate boundary lat/longs
                {
                  if (latitude[k]==(float)0.0) continue; // skip bad gps records
                  if (latitude[k] < lat1)       lat1 = latitude[k];
                  else {if (latitude[k] > lat2) lat2 = latitude[k];}
                  if (longitude[k] < long1)       long1 = longitude[k];
                  else {if (longitude[k] > long2) long2 = longitude[k];}
                }              
              thermal[++thermal_count] = new Climb(this);
              thermal[thermal_count].lat1 = lat1;
              thermal[thermal_count].long1 = long1;
              thermal[thermal_count].lat2 = lat2;
              thermal[thermal_count].long2 = long2;
              thermal[thermal_count].start_index = i;
              thermal[thermal_count].finish_index = j;
              i = j;
            }
        }
      if (thermal_count == IGCview.MAXTHERMALS) System.out.println("Too many thermals");
    }  

  private boolean thermalling(int i)  // true if thermalling at log index i
    {
      int j = i;
      float dist;       // distance travelled
      int time_taken;   // time taken between samples
      boolean in_thermal;

      if (latitude[i]==(float)0.0)
        { System.out.println(name+": Bad record in thermalling at "+i+", record_count = "+record_count);
          return false;
        }
      while (j < record_count && time[++j] - time[i] < IGCview.CLIMBAVG) {;}
      if (latitude[j]==(float)0.0)
        {
          while (j < record_count && latitude[j++] == (float)0.0) {;}
          j++;
        }
      if (j == record_count) return false;
      dist = IGCview.dec_to_dist(latitude[i], longitude[i], latitude[j], longitude[j]);
      time_taken = time[j] - time[i];
      in_thermal = (dist / time_taken * 3600 < IGCview.CLIMBSPEED);
      //if (time[i]>=44400 && time[i]<=44740)
      //  System.out.println("thermalling("+IGCview.format_clock(time[i])+") = "+in_thermal+
      //                     ", time_taken="+time_taken+", speed="+dist / time_taken * 3600 );
      return in_thermal;
    }

  // find_tp_times() scans through the logfile, identifying the control points for IGCview.task 
  void find_tp_times()
    {
      int i, imax=1;
      boolean reached_tp2 = false; // set to true if next TP after start successfully reached

      if (!gps_ok || IGCview.task==null || IGCview.task.tp_count<2) return;
      tps_rounded = 0;
      for (i=1; i<=record_count; i++) // scan forwards to tp[2]
        if (IGCview.task.in_sector(latitude[i], longitude[i], 2))
          {
            reached_tp2 = true;
            imax = i; // set imax to logpoint index in tp2 sector for reverse search for start time 
            break;
          }
      if (!reached_tp2) // if tp2 never reached, then set imax as furthest logpoint from start.
        {                      
          int furthest_index=1;
          float furthest_dist=(float)0.0;
          float dist;
          float start_latitude = IGCview.task.tp[1].latitude;
          float start_longitude = IGCview.task.tp[1].longitude;
          for (i=1; i<record_count; i++)
            {
              if (latitude[i]==(float)0.0) continue; // skip bad gps records
              dist = IGCview.dec_to_dist(latitude[i], longitude[i],
                                         start_latitude, start_longitude);
              if (dist>furthest_dist)
                {
                  furthest_dist = dist;
                  furthest_index = i;
                }
            }
          imax = furthest_index;
        }
      for (i=imax; i>1; i--)  // scan backwards from TP2 (or end) to find start
        if (IGCview.task.in_sector(latitude[i], longitude[i], 1))
          {  // START found in log, so set indexes accordingly
            tp_time[1] = time[i];
            tp_altitude[1] = altitude[i];
            tp_index[1] = i;
            if (IGCview.task.area[1]) // if a start AREA then set lat/long to actual logged point
              {
                tp_latitude[1] = latitude[i];
                tp_longitude[1] = longitude[i];
              }
            else         // otherwise use lat/long of start TP
              {
                tp_latitude[1] = IGCview.task.tp[1].latitude;
                tp_longitude[1] = IGCview.task.tp[1].longitude;
              }
            tps_rounded = 1; // flag a good start for next step of code below
            break;
          }
      if (tps_rounded>0) // if good start then count how many turnpoints reached
        for (i=tp_index[1]; i<=record_count; i++) // scan forwards from start
          if (IGCview.task.in_sector(latitude[i], longitude[i], tps_rounded+1))
            { // NEXT TP found in log, so set indexes accordingly
              tps_rounded++;
              tp_time[tps_rounded] = time[i];
              tp_altitude[tps_rounded] = altitude[i];
              tp_index[tps_rounded] = i;
              tp_latitude[tps_rounded] = latitude[i];
              tp_longitude[tps_rounded] = longitude[i];
              if (IGCview.task.area[tps_rounded]) // if tp an AREA then set lat/long to actual logged point
                {
                  tp_latitude[tps_rounded] = latitude[i];
                  tp_longitude[tps_rounded] = longitude[i];
                }
              else         // otherwise use lat/long of turnpoint
                {
                  tp_latitude[tps_rounded] = IGCview.task.tp[tps_rounded].latitude;
                  tp_longitude[tps_rounded] = IGCview.task.tp[tps_rounded].longitude;
                }
              if (tps_rounded == IGCview.task.tp_count) break; // quit loop when finish detected
            }
      // initialize tp_latitude[] / tp_longitude[] for remaining tp's not rounded
      for (int tp_num=tps_rounded+1; tp_num<=IGCview.task.tp_count; tp_num++)
        {
          tp_latitude[tp_num] = IGCview.task.tp[tp_num].latitude;
          tp_longitude[tp_num] = IGCview.task.tp[tp_num].longitude;
        }
      find_area_turnpoints(); // find lat,long of actual turns in area turnpoints
      find_landed_index();
    }

  // find_area_turnpoints() will set tp_latitude[tp_number] and tp_longitude[tp_number] as the lat,long for furthest
  // distance where IGCview.task.area[tp_number] is true (i.e. for each defined area tp)
  void find_area_turnpoints()
    {
      if (tps_rounded<=1) return; // quit here if just start tp or no start at all
      int iterations = 1; // loop counter to prevent infinite loop if find_middle_tps() bounces between solutions
      // set max_tp_number as last turnpoint to be calculated 
      int max_tp_number = (tps_rounded==IGCview.task.tp_count ? tps_rounded-1 : tps_rounded);
      // calculate middle turnpoints in a loop until steady solution is found
      while (find_middle_tps(max_tp_number) && iterations<10) iterations++;
      // IGCview.msgbox("Area points found in "+iterations+" iterations.");
      if (iterations==10) IGCview.msgbox("IGCview error: could not find solution for area task distance");
    }

  // find_middle_tps(max_tp) will set tp_latitude/longitude[2..max_tp] as the coordinates of actual turns through areas
  //  2..max_tp is all rounded turnpoints excluding start and finish
  boolean find_middle_tps(int max_tp) // will return true if call caused tp_latitude/longitude to change
    {
      // IGCview.msgbox("find_middle_tps(2.."+max_tp+")");
      boolean changed = false; // flag to detect whether this iteration has updated the coordinates of turns
      for (int tp_num=2; tp_num<=max_tp; tp_num++)
        {
          if (IGCview.task.area[tp_num])
            { if (find_area_tp(tp_num)) changed = true; }
          else
            {
              tp_latitude[tp_num] = IGCview.task.tp[tp_num].latitude;
              tp_longitude[tp_num] = IGCview.task.tp[tp_num].longitude;
            }
        }
      return changed;
    }
  
  // find_area_tp(tp_num) will set tp_latitude/longitude[tp_num] to best lat/long from logpoint for area TP
  // will return true if calculation results in changed coordinates
  boolean find_area_tp(int tp_num)
    {
      // IGCview.msgbox("find_area_tp("+tp_num+")");
      int prev_index = tp_index[tp_num]; // previous calculated index point at turn
      int min_index = tp_index[tp_num-1]; // start search from previous tp
      int max_index = (tp_num<tps_rounded ? tp_index[tp_num+1] : landed_index); // search to next TP or landed
      int furthest_index = 1;
      float furthest_dist=(float)0.0;
      float dist;
      for (int i = min_index; i<max_index; i++)
        {
          if (latitude[i]==(float)0.0) continue; // skip bad gps records
          if (!IGCview.task.in_sector(latitude[i], longitude[i], tp_num)) continue;
          // dist = sum of distances from previous turnpoint through this point to next turnpoint
          dist = IGCview.dec_to_dist(latitude[i], longitude[i],
                                     tp_latitude[tp_num-1], tp_longitude[tp_num-1]) +
                 IGCview.dec_to_dist(latitude[i], longitude[i],
                                     tp_latitude[tp_num+1], tp_longitude[tp_num+1]);
          if (dist>furthest_dist)
            {
              furthest_dist = dist;
              furthest_index = i;
            }
        }
      tp_index[tp_num] = furthest_index;
      tp_time[tp_num] = time[furthest_index];
      tp_altitude[tp_num] = altitude[furthest_index];
      tp_latitude[tp_num] = latitude[furthest_index];
      tp_longitude[tp_num] = longitude[furthest_index];
      return (tp_index[tp_num]!=prev_index);
    }
    
  void find_landed_index() // set landed_index of point of landing
    {
      int i=1;

      if (tps_rounded==0)
        while (++i<record_count && landed(i)) {;}
      else
        i = tp_index[1];
      while (++i<=record_count && !landed(i)) {;}
      landed_index = i;
      // reduce tps_rounded if landout detected earlier than each tp_index
      while (tps_rounded>1 && landed_index<tp_index[tps_rounded]) tps_rounded--;
      // if 'landout_max_distance' set then credit pilot with maximum distance flown rather than actual landout point
      if (tps_rounded>0 && tps_rounded < IGCview.task.tp_count && IGCview.config.landout_max_distance)
        {                      
          int nearest_index = landed_index; // index of log point nearest to turnpoint not quite reached
          float nearest_dist=(float)10000;  // will count backwards from landed index
          float dist;
          float next_tp_latitude = IGCview.task.tp[tps_rounded+1].latitude;
          float next_tp_longitude = IGCview.task.tp[tps_rounded+1].longitude;
          // scan backwards from landed index to last tp rounded to find min distance to next tp
          for (i=landed_index; i>=tp_index[tps_rounded]; i--)
            {
              if (latitude[i]==(float)0.0) continue; // skip bad gps records
              dist = IGCview.dec_to_dist(latitude[i], longitude[i],
                                         next_tp_latitude, next_tp_longitude);
              if (dist<=nearest_dist)
                {
                  nearest_dist = dist;
                  nearest_index = i;
                }
            }
          landed_index = nearest_index;
        }
    }

  boolean landed(int index)  // true if on ground at index
    {
      // glider speed averaged over LANDEDAVG seconds must be
      // below LANDEDSPEED for LANDEDTIME seconds (get it?)
      // glider also considered on ground for log time jump > LANDEDBREAK

      int i=index;

      while (i < record_count && time[i]-time[index] < IGCview.LANDEDTIME)
        if (latitude[i]!=(float)0.0 && get_speed(i) > IGCview.LANDEDSPEED)
          return false;
        else
          i++;
      return true;
    }

  float get_speed(int index) // return speed averaged over LANDEDAVG
    {
      int i1=index, i2;
      while (i1 <= record_count && latitude[i1]==(float)0.0) i1++; // skip bad gps records
      if (i1==record_count) return (float)0.0;
      i2 = i1;
      while (i2 <= record_count && time[i2]-time[i1] < IGCview.LANDEDAVG) i2++;
      while (i2 <= record_count && latitude[i2]==(float)0.0) i2++; // skip bad gps records
      if (i2==record_count || time[i2]-time[i1] > IGCview.LANDEDBREAK)
        return (float)0.0;
      else
        return IGCview.dec_to_dist(latitude[i1],
                                   longitude[i1],
                                   latitude[i2],
                                   longitude[i2]) / (time[i2]-time[i1]) * 3600;
    }

  // calculate leg_distance[1..IGCview.task.tp_count]
  void calc_leg_distances()
    {
      for (int tp_num = 1;  tp_num<IGCview.task.tp_count; tp_num++)
        {
          // tp_latitude/longitude will have been set to coords of TP center, or if an area then the TP log point
          leg_distance[tp_num] = IGCview.dec_to_dist(tp_latitude[tp_num], tp_longitude[tp_num],
                                                     tp_latitude[tp_num+1], tp_longitude[tp_num+1]);
        } 
    }
    
  void find_task_distance() // sets task_distance, task_time, task_speed
    {
      task_distance = (float) 0;
      task_time = 0;
      task_speed = (float) 0;
      if (IGCview.task==null || IGCview.task.tp_count==0) return;
      // calculate leg distances
      calc_leg_distances();
      // calculate task_distance
      if (tps_rounded==IGCview.task.tp_count)
        for (int tp_num=1; tp_num<=tps_rounded; tp_num++) task_distance += leg_distance[tp_num];
      else
        task_distance = index_to_dist(landed_index);
      // calculate task_time
      if (tps_rounded==IGCview.task.tp_count)
        task_time = time[tp_index[tps_rounded]]-time[tp_index[1]];
      else if (tps_rounded>=1)
        task_time = time[landed_index]-time[tp_index[1]];
      // calculate task_speed
      if (task_time!=0)
        task_speed = task_distance / task_time * 3600;
    }

  void find_climb_profile()
    {
      int i, box, total_time = 0;
      float climb_acc = 0;  // accumulated climb to calculate average

      if (!gps_ok || !baro_ok) return;
      int [] climb_time = new int [IGCview.CLIMBBOXES+1];
      find_thermals();
      for (box=1; box<=IGCview.CLIMBBOXES; box++) climb_time[box] = 0;
      for (i=1; i<=thermal_count; i++)
        {
          if (tps_rounded>0)
            if (thermal[i].finish_index < tp_index[1]) continue;  // not yet started task
            else if (tps_rounded==IGCview.task.tp_count &&       // completed task
                     thermal[i].finish_index > tp_index[tps_rounded]) break;
          total_time += thermal[i].duration();
          for (box=1; box<=IGCview.CLIMBBOXES; box++)
            {
              if (thermal[i].rate()<IGCview.CLIMBBOX* box)
                {
                  climb_time[box] += thermal[i].duration();
                  break;
                }
            }
        }
      for (box=1; box<=IGCview.CLIMBBOXES; box++) // calculate percentages
        climb_percent[box] = ((float) climb_time[box]) / total_time * 100;
      for (box=1; box<=IGCview.CLIMBBOXES; box++) // accumulate climb_acc
        climb_acc += IGCview.CLIMBBOX*((float)box-(float)0.5)*climb_percent[box];
      climb_avg = climb_acc / 100;
    }

  int time_to_index(int t) // do binary split to convert time to index
    {
      int i=record_count/2, i_min=1, i_max=record_count;
      while (time[i]>t || time[i+1]<t)
        {
          i = (i_min + i_max) / 2;
          if (i_max-i_min<=1) break;
          if (time[i]<t)
            i_min = i;
          else
            i_max = i;
        }
      return i;
    }

  // return index of log point nearest to distance d
  int dist_to_index(float d)
    { // do binary split
      int i=1, i_min=1, i_max=record_count;
      while (i_max-i_min>1)
        {
          i = (i_min + i_max) / 2;
          if (index_to_dist(i)<d)
            i_min = i;
          else
            i_max = i;
        }
      return i;
    }

  float index_to_dist(int index)
    {
      int tp_num=1;
      float dist, tp_dist=(float)0.0, tp_lat, tp_long;

      if (IGCview.task==null) return (float)0.0;
      while (tp_num<=tps_rounded && index>=tp_index[tp_num] && tp_num<IGCview.task.tp_count) 
        {
          tp_dist += leg_distance[tp_num];
          tp_num++;
        }
      dist = IGCview.dec_to_dist(latitude[index], longitude[index],
                                 tp_latitude[tp_num], tp_longitude[tp_num]);
      // after TP is rounded, cap distance away from next tp at next leg length
      if (dist>leg_distance[tp_num-1]) dist = leg_distance[tp_num-1];
      return tp_dist-dist;
    }

  void move_cursor1_time(int t)
    {
      int i = time_to_index(t);
      while (gps_ok && i<record_count && latitude[i]==(float)0.0) i++; // skip bad gps records
      IGCview.canvas.move_cursor1(i);
    }

  void move_cursor2_time(int t)
    {
      int i = time_to_index(t);
      while (gps_ok && i<record_count && latitude[i]==(float)0.0) i++; // skip bad gps records
      IGCview.canvas.move_cursor2(i);
    }

  void move_cursor1_dist(float d)
    {
      int i = dist_to_index(d);
      IGCview.canvas.move_cursor1(i);
    }

  void move_cursor2_dist(float d)
    {
      int i = dist_to_index(d);
      IGCview.canvas.move_cursor2(i);
    }

  void init_flight_data()
    {
      int tp_num;
      climb_avg = (float) 0.0;
      task_climb_avg = (float) 0.0;
      task_climb_height = (float) 0.0;
      task_climb_time = 0;
      task_climb_percent = (float) 0.0;
      task_climb_count = 0;
      task_time = 0;
      task_distance = (float) 0.0; // distance achieved around task (in nm)
      task_speed = (float) 0.0;
      task_cruise_avg = (float) 0.0;
      // initialize turnpoint data
      for (tp_num=1; tp_num<=IGCview.MAXTPS; tp_num++)
        {
          tp_index[tp_num] = 0;
          tp_time[tp_num] = 0;
          tp_altitude[tp_num] = 0;
        }
      // initialize leg data
      for (tp_num=1; tp_num<IGCview.MAXTPS; tp_num++)
        {
          leg_time[tp_num] = 0;
          leg_speed[tp_num] = (float) 0;
          leg_climb_avg[tp_num] = (float) 0;
          leg_climb_percent[tp_num] = (float) 0;
          leg_climb_count[tp_num] = 0;
          leg_cruise_avg[tp_num] = (float) 0;
          leg_distance[tp_num] = (float) 0;
        }
    }
  void calc_flight_data()
    {
      int tp_num;
      init_flight_data();
      find_thermals();
      find_tp_times();      // set tp_index[], tp_time[], tp_altitude[]
      find_task_distance(); // set task_time, task_distance, task_speed, leg_distance[]
      // calculate leg times and speeds
      for (tp_num=1; tp_num<tps_rounded; tp_num++)
       {
         leg_time[tp_num] = time[tp_index[tp_num+1]]-time[tp_index[tp_num]];
         leg_speed[tp_num] = leg_distance[tp_num] / leg_time[tp_num] * 3600;
       }
      calc_leg_climb_data();
      calc_leg_cruise_data();
    }

  void calc_leg_climb_data()  // calculate leg_average_climb[i], leg_climb_percent[i],
                              // task_climb_height, task_climb_avg, task_climb_time,
                              // task_climb_percent, task_climb_count
    {
      int leg=1, i=1;
      float leg_climb_height = (float) 0.0;
      int leg_climb_time = 0;

      task_climb_height = (float) 0.0;
      task_climb_time = 0;
      task_climb_count = 0;
      leg_climb_count[1] = 0;
      while (tps_rounded>0 && i<=thermal_count && thermal[i].finish_index<=tp_index[1])
        { 
          i++;
        } // pre-start
      while (i<=thermal_count+1)
	      {
	        if (i==thermal_count+1 || (leg < tps_rounded && 
                                   thermal[i].start_index>=tp_index[leg+1]))  // done leg
	          {
              if (leg_climb_time==0)
                {
                  leg_climb_avg[leg] = (float)0.0;
  	              leg_climb_percent[leg] = (float) leg_climb_time / leg_time[leg] * 100;
                }
              else
                {
	                leg_climb_avg[leg] = leg_climb_height / leg_climb_time * (float) 0.6;
                  if (leg_time[leg]!=0)
	                  leg_climb_percent[leg] = (float) leg_climb_time / leg_time[leg] * 100;
                  else leg_climb_percent[leg] = (float)0.0;
                }
  	          leg_climb_height = (float) 0.0;
	            leg_climb_time = 0;
	            if (++leg>=tps_rounded)
	              break;
		          leg_climb_count[leg] = 0;
	          }
          if (i<=thermal_count)
            {
	            task_climb_height += thermal[i].height();
	            task_climb_time += thermal[i].duration();
	            leg_climb_height += thermal[i].height();
	            leg_climb_time += thermal[i].duration();
	            leg_climb_count[leg]++;
              task_climb_count++;
              i++;
            }
	      }
      task_climb_avg = task_climb_height / task_climb_time * (float) 0.6;
      if (task_time!=0)
        task_climb_percent = (float) task_climb_time / task_time * 100;
      else
        task_climb_percent = (float)0.0;
    }

  void calc_leg_cruise_data()
   {
     float cruise_ratio;
     for (int leg=1; leg<=tps_rounded; leg++)
       {
         if (leg==IGCview.task.tp_count) break;
         cruise_ratio = ((float) 100 - leg_climb_percent[leg]) / 100;
         if (cruise_ratio==(float)0.0)
           leg_cruise_avg[leg] = (float)0.0;
         else
           leg_cruise_avg[leg] = leg_speed[leg] / cruise_ratio;
       }
     cruise_ratio = ((float)100 - task_climb_percent) / 100;
     if (cruise_ratio==(float)0.0)
       task_cruise_avg = (float)0.0;
     else
       task_cruise_avg = task_speed / cruise_ratio;
   }
     
}

//***************************************************************************//
//            TP_db                                                          //
//***************************************************************************//

class TP_db
{
  int tp_count = 0; // count of TP's in database and also index of last TP
  int current_tp_index = 0; // index of most recently selected TP in 'nearest_tp'
	float lat_min, long_min, lat_max, long_max, tp_lat, tp_long; // boundaries of TP area
  TP [] tp = new TP [IGCview.MAXTPDB];

  TP_db() {;}

  void load(String filename)
    {
     String buf;
		 tp_count = 0;
     try {
       IGCview.show_status("Loading TP's from '"+filename+"'...");
       URL gar_url;
       if(filename.startsWith("http")) gar_url=new URL(filename);
       else gar_url= new URL(IGCview.ApIGCview.getDocumentBase(), filename);

          BufferedReader in = new BufferedReader(
						new InputStreamReader(gar_url.openStream()));
          while ((buf = in.readLine()) != null) 
	      {
              if (buf.length()>0 && (buf.charAt(0) == 'W' || buf.charAt(0) == 'C'))
                { 
                  add(new TP(buf));
                  if (tp_count==IGCview.MAXTPDB) break;
                }
	      }
          in.close();
          IGCview.tps_loaded = true;
					IGCview.config.TPFILE = filename;
          calc_bounding_box(); // calculate lat,long min and max bounding TP area
          }
     catch (FileNotFoundException e) { System.err.println("TP_db.load: " + e);} 
     catch (MalformedURLException e) { System.err.println("TP_db.load: " + e);}
     catch (Exception e) { System.err.println("TP_db.load: " + e);}
     IGCview.show_status("");
    } 

  void add(TP new_tp)
    {
      if (tp_count==IGCview.MAXTPDB)
        {
          IGCview.msgbox("Adding "+new_tp.trigraph+
                              ". TP database limit reached. (="+IGCview.MAXTPDB+")");
          return;
        }
      tp[++tp_count] = new_tp;
      new_tp.index = tp_count;
    }

  void set_font(Graphics g) // set smaller font to TP name text on canvas
    {
      // Font font = g.getFont();
      // int style = font.getStyle();
      if (IGCview.ApIGCview.draw_images_mode)
        g.setFont(new Font("Helvetica", Font.BOLD, 12));
      else
        g.setFont(new Font("Helvetica", Font.PLAIN, 9));
    }

  void draw(Graphics g)
    {
      if (!IGCview.config.draw_tps) return;
      set_font(g); // set small font for TP names
      for (int i=1; i<=tp_count; i++) mark_tp(g, tp[i]);
    }

  private void mark_tp(Graphics g, TP tp)
    { 
      int x, y, w ,h;
      g.setColor(IGCview.config.TPCOLOUR.colour);
      x = IGCview.canvas.long_to_x(tp.longitude) - 3;
      y = IGCview.canvas.lat_to_y(tp.latitude) - 3;
      w = 6;
      h = 6;
      g.fillOval(x,y,w,h);
      if (IGCview.config.draw_tp_names)
        {
          g.setColor(IGCview.config.TPTEXT.colour);
          g.drawString(tp.trigraph, x+6, y);
        }
    }

  void mark_task_tp(Graphics g, TP tp)
    { 
      int x, y, w ,h;
      x = IGCview.canvas.long_to_x(tp.longitude) - 3;
      y = IGCview.canvas.lat_to_y(tp.latitude) - 3;
      w = 6;
      h = 6;
      g.fillOval(x,y,w,h);
      if (IGCview.config.draw_tp_names)
        {
          g.drawString(tp.trigraph, x+6, y);
        }
    }

  int lookup(String key)
  { int i;
    for (i=1; i<=tp_count; i++) if (tp[i].trigraph.startsWith(key)) return i;
    return -1;
  }

  // search(key) will return an html list of turnpoints, with each trigraph as a link to
  //  parent.select_tp(tp_index)
  String search(String key)
  { 
    String s = ""; // results string
    String k = key.toUpperCase();
    for (int i=1;i <= tp_count; i++)
      {
        if ((IGCview.tps.tp[i].trigraph+IGCview.tps.tp[i].full_name).toUpperCase().indexOf(k)!= -1)
          {
            s = s + "<tr>";
            s = s + "<td><a href='javascript:parent.select_tp("+i+")'>";
            s = s + "<b>" + tp[i].trigraph + "</b></a></td>";
            s = s + "<td>" + IGCview.trim_text(IGCview.tps.tp[i].full_name) + "</td>";
            s = s + "</tr>\n";
          }
      }
    if (s.equals(""))
      {
        s = "<font face='Helvetica'>IGCview searched both short name (trigraph) and full name,";
        s = s + " and found <font color='red'>no turnpoints</font> containing your search string '"+key+"'.</font>";
      }
    else
      {
        s = "<font face='Helvetica'><table>" + s + "</table></font>";
      }
    return s;
  }

  void nearest_tp(int x, int y)  // returns index of nearest TP to screen point x,y
    {
      float point_lat, point_long;
      int min_index = 1;
      float min_dist = 99999, dist;

      if (tp_count==0) return; // return if tp_db empty
      point_lat = IGCview.canvas.y_to_lat(y);
      point_long = IGCview.canvas.x_to_long(x);
      for (int i=1; i<=tp_count; i++) // check through TP's
        {
          dist = IGCview.dec_to_dist(point_lat, point_long, tp[i].latitude, tp[i].longitude);
          if (dist<min_dist)
            {
	            min_dist = dist;
              min_index = i;
            }
      	}
      current_tp_index = min_index;
    }

  void calc_bounding_box()   // zoom such that map displays task
    {
	    lat_min = tp[1].latitude;       // accumulate task bounding box
	    long_min = tp[1].longitude;
	    lat_max = lat_min+(float)0.1;
	    long_max = long_min+(float)0.1;
	    for (int i=2; i<=tp_count; i++)
	      {
	        tp_lat = tp[i].latitude;
	        tp_long = tp[i].longitude;
 	        if (tp_lat<lat_min)
		        lat_min = tp_lat;
 	        else if (tp_lat>lat_max)
		        lat_max = tp_lat;
 	        if (tp_long<long_min)
		        long_min = tp_long;
 	        else if (tp_long>long_max)
		        long_max = tp_long;
        }
    }

}

//***************************************************************************//
//            TP                                                             //
//***************************************************************************//

class TP
{
  String trigraph, full_name;
  float latitude, longitude;
  int index;

  TP() {;} // basic constructor for an empty TP

  TP(String file_rec) // gardown waypoint record or .igc 'C' record
    {
      if (file_rec.startsWith("C"))
        igc_tp(file_rec);
      else
        gdn_tp(file_rec);
    }

  void igc_tp(String igc_rec)
    {
      trigraph = igc_rec.substring(18);
      //trigraph=trigraph.toUpperCase();
      full_name = igc_rec.substring(18);
      latitude = IGCview.decimal_latlong(igc_rec.substring(1,3),
                                 igc_rec.substring(3,5)+"."+
                                 igc_rec.substring(5,8),
                                 igc_rec.charAt(8));
      longitude = IGCview.decimal_latlong(igc_rec.substring(9,12),
                                  igc_rec.substring(12,14)+"."+
                                  igc_rec.substring(14,17),
                                  igc_rec.charAt(17));
    }

  void gdn_tp(String gdn_rec)
    {
      trigraph = gdn_rec.substring(3,9).toUpperCase();
      full_name = gdn_rec.substring(60);
      latitude = IGCview.decimal_latlong(gdn_rec.substring(11,13),
                                 gdn_rec.substring(14,21),
                                 gdn_rec.charAt(10));
      longitude = IGCview.decimal_latlong(gdn_rec.substring(23,26),
                                  gdn_rec.substring(27,34),
                                  gdn_rec.charAt(22));
    }

  boolean equals(TP t)
    {
      if (t.latitude != latitude) return false;
      if (t.longitude != longitude) return false;
      return true;
    }
} // end of class TP


//***************************************************************************//
//            Task                                                           //
//***************************************************************************//

class Task
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

//***************************************************************************//
//            Zoom                                                           //
//***************************************************************************//
//
//  Implements zoom history as circular buffer of size ZOOMHISTORY
//  Current scale in 'zoom.xscale' and 'zoom.yscale'
//

class Zoom
{
  // public class variables:
  public float xscale, yscale, latitude, longitude;

  // constants:
  private static final int ZOOMHISTORY = 20;
  private static final float INRATIO = (float) 2, OUTRATIO = (float) 0.5;
  // circular zoom buffer:
  private float [] x_hist = new float [21];  // ZOOMHISTORY + 1
  private float [] y_hist = new float [21];  // ZOOMHISTORY + 1
  private float [] lat_hist = new float [21];  // ZOOMHISTORY + 1
  private float [] long_hist = new float [21];  // ZOOMHISTORY + 1

  float reset_latitude, reset_longitude, reset_xscale;

  private int current, in_limit, out_limit;

  Zoom(float latitude, float longitude, float xscale)
    {
      reset_latitude = latitude;
      reset_longitude = longitude;
      reset_xscale = xscale;
      zoom_reset();
    }

  void zoom_reset()
    {
      latitude = reset_latitude;
      longitude = reset_longitude;
      xscale = reset_xscale;
      yscale = xscale * 
               IGCview.dec_to_dist(latitude, longitude, latitude+1, longitude) /
               IGCview.dec_to_dist(latitude, longitude, latitude, longitude+1);
      current = 1;
      in_limit = 1;
      out_limit = 1;
      x_hist[1] = xscale;
      y_hist[1] = yscale;
      lat_hist[1] = latitude;
      long_hist[1] = longitude;
    }

  void scale_corner(float lat, float lon, float factor)   // adjust scale by 'factor'
    {
      xscale *= factor;
      yscale *= factor;
      latitude = lat;
      longitude = lon;
      if (factor > (float) 1)
        { // zooming IN
          if (current++ == ZOOMHISTORY) current = 1;
          in_limit = current;
        }
      else
        { // zooming OUT
          if (current-- == 1) current = ZOOMHISTORY;
          out_limit = current;
        }
      x_hist[current] = xscale;
      y_hist[current] = yscale;
      lat_hist[current] = latitude;
      long_hist[current] = longitude;
    }
void
scale_center(float factor)   // adjust scale by 'factor'
{
  latitude -= (float)(.5*IGCview.height/yscale);
  longitude += (float)(.5*IGCview.width/xscale);
  xscale *= factor;
  yscale *= factor;
  latitude += (float)(.5*IGCview.height/yscale);
  longitude -= (float)(.5*IGCview.width/xscale);

      if (factor > (float) 1)
        { // zooming IN
          if (current++ == ZOOMHISTORY) current = 1;
          in_limit = current;
        }
      else
        { // zooming OUT
          if (current-- == 1) current = ZOOMHISTORY;
          out_limit = current;
        }
      x_hist[current] = xscale;
      y_hist[current] = yscale;
      lat_hist[current] = latitude;
      long_hist[current] = longitude;
    }

  void zoom_to_task()   // zoom such that map displays task
    {
	float lat_min, long_min, lat_max, long_max, tp_lat, tp_long;

	if (IGCview.task==null) return;

	lat_min = IGCview.task.tp[1].latitude;       // accumulate task bounding box
	long_min = IGCview.task.tp[1].longitude;
	lat_max = lat_min+(float)0.1;
	long_max = long_min+(float)0.1;
	for (int tp=2; tp<=IGCview.task.tp_count; tp++)
	  {
	    tp_lat = IGCview.task.tp[tp].latitude;
	    tp_long = IGCview.task.tp[tp].longitude;
 	    if (tp_lat<lat_min)
		lat_min = tp_lat;
 	    else if (tp_lat>lat_max)
		lat_max = tp_lat;
 	    if (tp_long<long_min)
		long_min = tp_long;
 	    else if (tp_long>long_max)
		long_max = tp_long;
        }
      zoom_to_box(lat_min, long_min, lat_max, long_max);
    }

  void zoom_to_flight()   // zoom such that map displays task
    {
	float lat_min, long_min, lat_max, long_max, point_lat, point_long;
      int i=1;
 	TrackLog log;

	if (IGCview.primary_index==0) return; // do nothing if no trace loaded

	log=IGCview.logs[IGCview.primary_index];

	while (((log.latitude[i]==(float)0.0) ||
		 (log.longitude[i]==(float)0.0)) &&
             (i<log.record_count)) i++; // skip bad gps records
	lat_min = log.latitude[i];       // accumulate task bounding box
	long_min = log.longitude[i];
	lat_max = lat_min+(float)0.1;
	long_max = long_min+(float)0.1;
	while (++i<log.record_count)
	  {
	    if ((log.latitude[i]==(float)0.0) ||
		  (log.longitude[i]==(float)0.0)) continue; // skip bad gps recs
	    point_lat = log.latitude[i];
	    point_long = log.longitude[i];
 	    if (point_lat<lat_min)
		lat_min = point_lat;
 	    else if (point_lat>lat_max)
		lat_max = point_lat;
 	    if (point_long<long_min)
		long_min = point_long;
 	    else if (point_long>long_max)
		long_max = point_long;
        }
      zoom_to_box(lat_min, long_min, lat_max, long_max);
    }


void
zoom_to_home()
// zoom such that map displays both task and track
{
  float lat_min, long_min, lat_max, long_max, lat, lon;
  int i=1;
  TrackLog log;

  if (IGCview.primary_index==0) return; // do nothing if no trace loaded
  
  log=IGCview.logs[IGCview.primary_index];
  while (((log.latitude[i]==(float)0.0) ||
	  (log.longitude[i]==(float)0.0)) &&
	 (i<log.record_count)) i++; // skip bad gps records
  lat_min = log.latitude[i];       // accumulate task bounding box
  long_min = log.longitude[i];
  lat_max = lat_min+(float)0.1;
  long_max = long_min+(float)0.1;
  while (++i<log.record_count) {
    if ((log.latitude[i]==(float)0.0) ||
	(log.longitude[i]==(float)0.0)) continue; // skip bad gps recs
    lat = log.latitude[i];
    lon = log.longitude[i];
    if (lat<lat_min) lat_min = lat;
    else if (lat>lat_max) lat_max = lat;
    if (lon<long_min) long_min = lon;
    else if (lon>long_max) long_max = lon;
  }
  
  if (null!=IGCview.task)
    for (int tp=2; tp<=IGCview.task.tp_count; tp++) {
      lat = IGCview.task.tp[tp].latitude;
      lon = IGCview.task.tp[tp].longitude;
      if (lat<lat_min) lat_min = lat;
      else if (lat>lat_max) lat_max = lat;
      if (lon<long_min) long_min = lon;
      else if (lon>long_max) long_max = lon;
    }

  zoom_to_box(lat_min, long_min, lat_max, long_max);
  // zoom_to_box does not quite zoom to the given rect; however, the scale
  // is ok, so we can simply recenter.
  center((float)((lat_min+lat_max)/2),
	 (float)((long_min+long_max)/2));
}

  
  void zoom_to_box(float lat_min, float long_min, float lat_max, float long_max) // zoom such that map fits bounds
    {
      float  dist_x, dist_y;

      dist_x = IGCview.dec_to_dist(lat_min,long_min,lat_min,long_max);
      dist_y = IGCview.dec_to_dist(lat_min,long_min,lat_max,long_min);
      if (dist_x/IGCview.width > dist_y/IGCview.height)
        {
          xscale = (float) (IGCview.width-40) / (long_max-long_min);
          yscale = xscale * 
                   IGCview.dec_to_dist(lat_min, long_min, lat_min+1, long_min) /
                   IGCview.dec_to_dist(lat_min, long_min, lat_min, long_min+1);
        }
      else
        {
          yscale = (float) (IGCview.height-40) / (lat_max-lat_min);
          xscale = yscale * 
                   IGCview.dec_to_dist(lat_min, long_min, lat_min, long_min+1) /
                   IGCview.dec_to_dist(lat_min, long_min, lat_min+1, long_min);
        }
      //latitude = lat_max + (float) 20 / yscale;
      //longitude = long_min - (float) 20 / xscale;
      latitude=(float)((lat_max+lat_min)/2+.5*IGCview.height/yscale);
      longitude=(float)((long_max+long_min)/2-.5*(IGCview.width-40)/xscale);

      current = 1;
      in_limit = 1;
      out_limit = 1;
      x_hist[1] = xscale;
      y_hist[1] = yscale;
      lat_hist[1] = latitude;
      long_hist[1] = longitude;
    }

  void zoom_out()
    {
      if (current == out_limit)
        {
          scale_center(OUTRATIO);
          return;
        }
      if (current-- == 1) current = ZOOMHISTORY;
      xscale = x_hist[current];
      yscale = y_hist[current];
      latitude = lat_hist[current];
      longitude = long_hist[current];
    }

  void zoom_in()
    {
      if (current == in_limit)
        {
          scale_center(INRATIO);
          return;
        }
      if (current++ == ZOOMHISTORY) current = 1;
      xscale = x_hist[current];
      yscale = y_hist[current];
      latitude = lat_hist[current];
      longitude = long_hist[current];
    }

void
pan(float latd, float lond)
// pan: move map by (latd,lond)
// any panning resets zoom history
{
  latitude+=latd;
  longitude+=lond;
  current = 1;
  in_limit = 1;
  out_limit = 1;
  x_hist[1] = xscale;
  y_hist[1] = yscale;
  lat_hist[1] = latitude;
  long_hist[1] = longitude;
}
void
center(float lat, float lon)
// Pan so that (lat,lon) is in the center of the map.
{
  lat=(float)(lat+.5*IGCview.height/yscale);
  lon=(float)(lon-.5*IGCview.width/xscale);
  if(lat==latitude
     && lon==longitude) return;
  latitude=lat;
  longitude=lon;
  current=1;
  in_limit = 1;
  out_limit = 1;
  x_hist[1] = xscale;
  y_hist[1] = yscale;
  lat_hist[1] = latitude;
  long_hist[1] = longitude;	
}
boolean
inview(float lat, float lon)
// Pan if needed so that point (lat,lon) is inview.
// Pan by 1/4 of the view size, if that would not
// get the point in view, then center map around the pt.
{
  //  return true;
  if(latitude-IGCview.height/yscale<lat && lat<latitude
     && longitude<lon && lon<longitude+IGCview.width/xscale)
    return true;

  if(lat>=latitude) // pan N
    pan((float)(0.25*IGCview.height/yscale),(float)0.0);
  else if(lat<=latitude-IGCview.height/yscale) // pan N
    pan((float)(-0.25*IGCview.height/yscale),(float)0.0);
  if(longitude>=lon) // pan W
    pan((float)0.0,(float)(-0.25*IGCview.width/xscale));
  else if(lon>=longitude+IGCview.width/xscale)
    pan((float)0.0,(float)(0.25*IGCview.width/xscale));

  if(latitude-IGCview.height/yscale<lat && lat<latitude
          && longitude<lon && lon<longitude+IGCview.width/xscale)
        return false;

  center(lat,lon);
  return false;
}
}

//***************************************************************************//
//            Climb                                                          //
//***************************************************************************//

class Climb
{
  float lat1, long1, lat2, long2;  // boundaries of circling flight
  int start_index, finish_index;   // first and last log point
  TrackLog track_log;

  Climb(TrackLog t)
    {
      track_log = t;
    }

  int duration()
    {
      return track_log.time[finish_index] - track_log.time[start_index];
    }

  float rate()
    {
      return height() / duration() * (float) 0.6;
    }

  float height()
    {
      return track_log.altitude[finish_index]-track_log.altitude[start_index];
    }
}

//***************************************************************************//
//             SelectFrame                                                   //
//***************************************************************************//

class SelectFrame extends Frame implements WindowListener
{

  SelectWindow window;

  private static String [] [] menus = {
    {"File", "Close"}
  };    

  private int width, height;

  SelectFrame ()
    {
      width = IGCview.config.SELECTWIDTH;
      height = IGCview.config.SELECTHEIGHT;
      setSize(width, height);
      this.addWindowListener(this);
      ScrollPane pane = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
      this.add (pane, "Center");
      window = new SelectWindow(this);
      pane.add(window);

      MenuBar menubar = new MenuBar ();
      this.setMenuBar (menubar);

      for (int c = 0; c < menus.length; c++) {
        Menu m = new Menu (menus [c][0]);
        menubar.add (m);
        for (int r = 1; r < menus [c] .length; r++) {
  	  if (menus [c][r] == null)
              m.addSeparator ();
	  else
            {
	      MenuItem i = new MenuItem (menus [c][r]);
	      m.add (i);
	      i.setActionCommand (menus [c][r] .toLowerCase ());
	      i.addActionListener (window);
  	    }
        }
      }
      this.pack ();
    }

  public Dimension getPreferredSize ()
    {
      return new Dimension (width, height);
    }

  // WindowListener interface:
  public void windowActivated(WindowEvent e) {;}
  public void windowClosed(WindowEvent e) {;}
  public void windowClosing(WindowEvent e)
    {
      dispose();
	IGCview.select_window = null;
    }

  public void windowDeactivated(WindowEvent e) {;}
  public void windowDeiconified(WindowEvent e) {;}
  public void windowIconified(WindowEvent e) {;}
  public void windowOpened(WindowEvent e) {;}

}

//***************************************************************************//
//            SelectWindow                                                   //
//***************************************************************************//

class SelectWindow extends Panel implements ActionListener
{
  private Checkbox [] primary = new Checkbox [IGCview.log_count+1];
  private CheckboxGroup primary_group = new CheckboxGroup();
  private Checkbox [] secondary = new Checkbox [IGCview.log_count+1];
  private Button ok_button, cancel_button;
  private SelectFrame select_frame;              // frame to draw on

  Button make_button(String name, GridBagLayout gridbag, GridBagConstraints c) 
    {
      Button b = new Button(name);
      gridbag.setConstraints(b, c);
      b.addActionListener(this);
      add(b);
      return b;
    }

  SelectWindow(SelectFrame select_frame)
    {
      Label l;
	String flags;

      this.select_frame = select_frame;
 
      GridBagLayout gridbag = new GridBagLayout();
      GridBagConstraints c = new GridBagConstraints();
 
      setFont(new Font("Helvetica", Font.PLAIN, 14));
      setLayout(gridbag);
   
      c.insets = new Insets(3,3,3,3);                 // space 3 pixels around fields
      // c.fill = GridBagConstraints.BOTH;
      c.anchor = GridBagConstraints.WEST;

      c.weightx = 0.0;
      l = new Label("Primary");
      gridbag.setConstraints(l,c);
      add(l);
      l = new Label("Secondary");
      c.gridwidth = GridBagConstraints.RELATIVE;
      gridbag.setConstraints(l,c);
      add(l);
      c.weightx = 1.0;
      l = new Label("Flight");
      c.gridwidth = GridBagConstraints.REMAINDER;
      gridbag.setConstraints(l,c);
      add(l);
      for (int i=1; i<=IGCview.log_count; i++)
        {
          c.weightx = 0.0;
          c.gridwidth = 1;
          primary[i] = new Checkbox("", false, primary_group);
          gridbag.setConstraints(primary[i],c);
          add(primary[i]);
          if (i==IGCview.primary_index) primary[i].setState(true);
	    flags = (IGCview.logs[i].baro_ok) ? "Baro " : "";
	    if (IGCview.logs[i].gps_ok) flags = flags+"GPS";
          secondary[i] = new Checkbox(flags);
          c.gridwidth = GridBagConstraints.RELATIVE;
          gridbag.setConstraints(secondary[i],c);
          add(secondary[i]);
          secondary[i].setState(IGCview.secondary[i]);
          c.weightx = 1.0;
          l = new Label(IGCview.format_date(IGCview.logs[i].date)+" "+IGCview.logs[i].name);
          c.gridwidth = GridBagConstraints.REMAINDER;
          gridbag.setConstraints(l,c);
          add(l);
        }
      c.weightx = 0.0;
      c.gridwidth = GridBagConstraints.RELATIVE;
      cancel_button = make_button("Cancel", gridbag,c);  //-- BUTTON: Cancel
      ok_button = make_button("OK", gridbag,c);       //-- BUTTON: OK
    }

  public void actionPerformed(ActionEvent e)
    {
      String label = e.getActionCommand();
      if (label.equals("OK"))
        { 
          ok_hit();
	    IGCview.canvas.paint(IGCview.canvas.getGraphics());
          select_frame.dispose();
          IGCview.select_window = null;
          IGCview.logs[IGCview.primary_index].set_primary();
          return;
        }
      else if (label.equals("Cancel"))
  	  {
          select_frame.dispose();
          IGCview.select_window = null;
          return;
        }
      else if (label.equals ("close"))
        {
          select_frame.dispose();
          IGCview.select_window = null;
          return;
        }
      else if (label.equals ("exit")) IGCview.exit();
    }

  void ok_hit()
    { 
      int i;
      for (i=1; i<=IGCview.log_count; i++)
        {
          IGCview.secondary[i] = false;
          if (primary[i].getState()) 
            {
              IGCview.primary_index = i;
            }
          else if (secondary[i].getState())
            {
              IGCview.secondary[i] = true;
            }
        }
      IGCview.sec_log.climb_avg = (float) 0.0; // reset secondary track log
    }
}

//***************************************************************************//
//            Secondary TrackLog: TrackLogS                                  //
//***************************************************************************//

class TrackLogS
{
  float [] climb_percent = new float [IGCview.CLIMBBOXES+1];
  float climb_avg = (float) 0.0;
  float task_climb_avg = (float) 0.0;
  float task_climb_height = (float) 0.0;
  int   task_climb_time = 0;
  float task_climb_percent = (float) 0.0;
  int   task_climb_count = 0;
  int   task_time = 0;
  float task_distance = (float) 0.0;
  float task_speed = (float) 0.0;
  float task_cruise_avg = (float) 0.0;

  int   [] tp_time  = new int [IGCview.MAXTPS+1];      // time rounded each TP
  float [] tp_altitude  = new float [IGCview.MAXTPS+1]; // height rounded each TP
  int   [] leg_time = new int [IGCview.MAXTPS];        // duration for each leg
  float [] leg_speed = new float [IGCview.MAXTPS];       // speed for each leg
  float [] leg_climb_avg = new float [IGCview.MAXTPS];      // average climb for each leg
  float [] leg_climb_percent = new float [IGCview.MAXTPS];  // percent time climbing for each leg
  int   [] leg_climb_count = new int [IGCview.MAXTPS];  // percent time climbing for each leg
  float [] leg_cruise_avg = new float [IGCview.MAXTPS];     // average cruise speed for each leg
  float [] leg_distance = new float [IGCview.MAXTPS];  // achieved length of each actual leg. Differs from task
                                                       // leg distances when area tp's are in the task

  int tps_rounded = 0;                                 // TPs successfully rounded

  private float climb_acc = (float) 0.0;

  // TrackLogS constructor:
  
  TrackLogS()
    {;}

  public void draw_climb_profile(Graphable cp_canvas, Graphics g)
    {
      int sec_log_count=0,x,y, log_num, box;
      Polygon p = new Polygon();
      Color c = IGCview.config.SECAVGCOLOUR.colour;
      log_num = 1;
      while (log_num <= IGCview.MAXLOGS && !IGCview.secondary[log_num++]) {;}
      if (log_num>IGCview.MAXLOGS) return;
      if (climb_avg == (float) 0.0)
        {
          for (box=1; box<=IGCview.CLIMBBOXES; box++) climb_percent[box] = (float) 0.0;
          for (log_num=1; log_num<=IGCview.MAXLOGS; log_num++) // accumulate time_percents
                                                               //  in boxes
            if (IGCview.secondary[log_num] &&
                IGCview.logs[log_num].baro_ok &&
                IGCview.logs[log_num].gps_ok)
              {
                sec_log_count++;
                IGCview.logs[log_num].find_climb_profile();
                for (box=1; box<=IGCview.CLIMBBOXES; box++)
                  climb_percent[box] += IGCview.logs[log_num].climb_percent[box];
              }
          climb_acc = (float) 0.0;
          for (box=1; box<=IGCview.CLIMBBOXES; box++) // divide boxes by sec_log_count
            {                                                   //   to get averages
              climb_percent[box] /= sec_log_count;
              climb_acc += IGCview.CLIMBBOX*((float)box-(float)0.5)*climb_percent[box];
            }
          climb_avg = climb_acc / 100;
        }
      x = cp_canvas.climb_to_x((float) 0);                  // draw average profile
      y = cp_canvas.get_y((float) 0);
      p.addPoint(x,y);
      y = cp_canvas.get_y(climb_percent[1]);
      p.addPoint(x,y);
      for (box=1; box<=IGCview.CLIMBBOXES; box++)
        { 
          x = cp_canvas.box_to_x(box);
          y = cp_canvas.get_y(climb_percent[box]);
          p.addPoint(x,y);
        }
      x = cp_canvas.box_to_x(IGCview.CLIMBBOXES);
      y = cp_canvas.get_y((float) 0);
      p.addPoint(x,y);
      x = cp_canvas.climb_to_x(climb_avg);
      g.setColor(IGCview.config.SECAVGCOLOUR.colour);                           // Fill graph
      g.fillPolygon(p);
      g.drawLine(x,0,x,cp_canvas.get_y((float) 0));  // Line for avg climb
      g.drawLine(x+1,0,x+1,cp_canvas.get_y((float) 0));
      for (log_num=1; log_num<=IGCview.MAXLOGS; log_num++)       // draw secondary profiles
        if (IGCview.secondary[log_num])
            IGCview.logs[log_num].draw_climb_profile(cp_canvas,g,false);
    }

  public void draw_alt(Graphable alt_canvas, Graphics g)
    {
      for (int log_num=1; log_num<=IGCview.MAXLOGS; log_num++)   // draw secondary profiles
        if (IGCview.secondary[log_num]) IGCview.logs[log_num].draw_alt(alt_canvas,g,false);
 
    }

  public void draw_climb(Graphable climb_canvas, Graphics g)
    {
      for (int log_num=1; log_num<=IGCview.MAXLOGS; log_num++)   // draw secondary climbs
        if (IGCview.secondary[log_num])
          IGCview.logs[log_num].draw_climb(climb_canvas,g,false);
    }

  public void draw_cruise(Graphable canvas, Graphics g)
    {
      for (int log_num=1; log_num<=IGCview.MAXLOGS; log_num++)   // draw secondary speeds
        if (IGCview.secondary[log_num])
          IGCview.logs[log_num].draw_cruise(canvas,g,false);
    }

  void calc_flight_data()
    {
      int i,log_num;
      TrackLog log;
      int count_task_climb_avg = 0,
	    count_task_climb_height = 0,
	    count_task_climb_time = 0,
	    count_task_climb_percent = 0,
	    count_task_climb_count = 0,
	    count_task_time = 0,
	    count_task_distance = 0,
	    count_task_speed = 0,
	    count_task_cruise_avg = 0;
      int [] count_tp_time = new int [IGCview.MAXTPS+1];
      int [] count_tp_altitude = new int [IGCview.MAXTPS+1];
      int [] count_leg_time = new int [IGCview.MAXTPS];
      int [] count_leg_speed = new int [IGCview.MAXTPS];
      int [] count_leg_climb_avg = new int [IGCview.MAXTPS];
      int [] count_leg_climb_percent = new int [IGCview.MAXTPS];
      int [] count_leg_climb_count = new int [IGCview.MAXTPS];
      int [] count_leg_cruise_avg = new int [IGCview.MAXTPS];
      int [] count_leg_distance = new int [IGCview.MAXTPS];

      task_climb_avg = (float) 0.0;
      task_climb_height = (float) 0.0;
      task_climb_time = 0;
      task_climb_percent = (float) 0.0;
      task_climb_count = 0;
      task_time = 0;
      task_distance = (float) 0.0;
      task_speed = (float) 0.0;
      task_cruise_avg = (float) 0.0;

      // initialise tp counters and values
      for (i=1; i<=IGCview.MAXTPS; i++)
        {
          count_tp_time[i] = 0;
          tp_time[i] = 0;
          count_tp_altitude[i] = 0;
          tp_altitude[i] = 0;
        }
      // initialise leg counters and values
	    for (i=1; i<IGCview.MAXTPS; i++)
	      {
	        leg_time[i] = 0;
	        leg_speed[i] = (float) 0.0;
          leg_climb_avg[i] = (float) 0.0;
	        leg_climb_percent[i] = (float) 0.0;
          leg_climb_count[i] = 0;
	        leg_cruise_avg[i] = (float) 0.0;	    
	        leg_distance[i] = (float) 0.0;	    
	        count_leg_time[i] = 0;
	        count_leg_speed[i] = 0;
          count_leg_climb_avg[i] = 0;
	        count_leg_climb_percent[i] = 0;
          count_leg_climb_count[i] = 0;
	        count_leg_cruise_avg[i] = 0;
	        count_leg_distance[i] = 0;
	      }
	
      for (log_num=1; log_num<=IGCview.MAXLOGS; log_num++)
        if (IGCview.secondary[log_num])
         {
            log = IGCview.logs[log_num];
            if (!log.gps_ok) continue; // skip log if no gps
            log.calc_flight_data();
            if (log.task_climb_avg!=(float)0.0)
              {
                task_climb_avg += log.task_climb_avg;
                count_task_climb_avg++;
              }
            if (log.task_climb_height!=(float)0.0)
              {
                task_climb_height += log.task_climb_height;
                count_task_climb_height++;
              }
            if (log.task_climb_time!=0)
              {
                task_climb_time += log.task_climb_time;
                count_task_climb_time++;
              }
            if (log.task_climb_percent!=(float)0.0)
              {
                task_climb_percent += log.task_climb_percent;
                count_task_climb_percent++;
              }
            if (log.task_climb_count!=0)
              {
                task_climb_count += log.task_climb_count;
                count_task_climb_count++;
              }
            if (log.task_time!=0)
              {
                task_time += log.task_time;
                count_task_time++;
              }
            if (log.task_distance!=(float)0.0)
              {
                task_distance += log.task_distance;
                count_task_distance++;
              }
            if (log.tps_rounded==IGCview.task.tp_count)
              {
                task_speed += log.task_speed;
                count_task_speed++;
              }
            if (log.task_cruise_avg!=(float)0.0)
              {
                task_cruise_avg += log.task_cruise_avg;
                count_task_cruise_avg++;
              }
            // accumulate tp counts and values
            for (i=1; i<=log.tps_rounded; i++)
              {
                count_tp_time[i]++;
                tp_time[i] += log.tp_time[i];
                if (log.baro_ok)
                  {
                    count_tp_altitude[i]++;
                    tp_altitude[i] += log.tp_altitude[i];
                  }
              }
            // accumulate leg counts and values
            for (i=1; i<IGCview.MAXTPS; i++)
              {
                if (log.tps_rounded>i) // only accumlate values for legs that have been completed
                  {
                    count_leg_time[i]++;
                    leg_time[i] += log.leg_time[i];
                    count_leg_speed[i]++;
                    leg_speed[i] += log.leg_speed[i];
                    count_leg_climb_count[i]++;
                    leg_climb_count[i] += log.leg_climb_count[i];
                    count_leg_cruise_avg[i]++;
                    leg_cruise_avg[i] += log.leg_cruise_avg[i];
                    count_leg_climb_percent[i]++;
                    leg_climb_percent[i] += log.leg_climb_percent[i];
                    count_leg_distance[i]++;
                    leg_distance[i] += log.leg_distance[i];
                  }
                if (log.tps_rounded>i && log.task_climb_avg!=(float)0.0)
                  {
                    count_leg_climb_avg[i]++;
                    leg_climb_avg[i] += log.leg_climb_avg[i];
                  }
              }
         }
      if (count_task_climb_avg!=0) task_climb_avg /= count_task_climb_avg;
      if (count_task_climb_height!=0) task_climb_height /= count_task_climb_height;
      if (count_task_climb_time!=0) task_climb_time /= count_task_climb_time;
      if (count_task_climb_percent!=0) task_climb_percent /= count_task_climb_percent;
      if (count_task_climb_count!=0) task_climb_count /= count_task_climb_count;
      if (count_task_time!=0) task_time /= count_task_time;
      if (count_task_distance!=0) task_distance /= count_task_distance;
      if (count_task_speed!=0) task_speed /= count_task_speed;
      if (count_task_cruise_avg!=0) task_cruise_avg /= count_task_cruise_avg;
      for (i=1; i<=IGCview.MAXTPS; i++)
        {
          if (count_tp_time[i]!=0) tp_time[i] /= count_tp_time[i];
          if (count_tp_altitude[i]!=0) tp_altitude[i] /= count_tp_altitude[i];
        }    
      for (i=1; i<IGCview.MAXTPS; i++)
        {
          if (count_leg_time[i]!=0) leg_time[i] /= count_leg_time[i];
          if (count_leg_speed[i]!=0) leg_speed[i] /= count_leg_speed[i];
          if (count_leg_climb_avg[i]!=0) leg_climb_avg[i] /= count_leg_climb_avg[i];
          if (count_leg_climb_percent[i]!=0) leg_climb_percent[i] /= count_leg_climb_percent[i];
          if (count_leg_climb_count[i]!=0) leg_climb_count[i] /= count_leg_climb_count[i];
          if (count_leg_cruise_avg[i]!=0) leg_cruise_avg[i] /= count_leg_cruise_avg[i];
          if (count_leg_distance[i]!=0) leg_distance[i] /= count_leg_distance[i];
        }
    }
}

 // end of TrackLogS

//***************************************************************************//
//            CursorWindow                                                   //
//***************************************************************************//

class CursorWindow
{
  static private float task_dist1=(float)0.0;
  static private float task_dist2=(float)0.0;
  static private int time1=0;
  static private int time2=0;
  static private int time_diff=0;
  static private float alt1=(float)0.0;
  static private float alt2=(float)0.0;
  static private float alt_diff=(float)0.0;
  static private float climb=(float)0.0;
  static private float dist=(float)0.0;
  static private float speed_knots =(float)0.0;
  static private float l_d=(float)0.0;
  static private int mode_time1=0;
  static private int mode_time2=0;
  static private int mode_time_diff=0;
  static private float mode_alt1=(float)0.0;
  static private float mode_alt2=(float)0.0;
  static private float mode_alt_diff=(float)0.0;
  static private float mode_climb=(float)0.0;
  static private float mode_dist=(float)0.0;
  static private float mode_speed_knots =(float)0.0;
  static private float mode_l_d=(float)0.0;
  static boolean thermalling = false;  // mode flag: thermalling / cruising

  CursorWindow() {;}

  public static void update()
    {
	String cdata = "";
	String s;
	String dist_units;
	String alt_units;
	String climb_units;
  String td_l = "<tr bgcolor=#00ffff><td>"; // string for left-aligned TD element
  String td_r = "<tr bgcolor=#00ff00><td align='right'>"; // string for right-aligned TD element
  String td_end = "</td></tr>";
  
      calc_values();

      dist_units = "km";
      if (IGCview.config.convert_task_dist==(float)1.0) dist_units = "nm";
      else if (IGCview.config.convert_task_dist==IGCview.NMMI) dist_units = "mi";

    	cdata = "<html><head><style> td { font-size: 10pt; font-family: sans-serif; }</style></head>";

      cdata = cdata + "<body leftmargin='2'>";
      cdata = cdata + "<table width='100%' border='0' cellpadding='1' cellspacing='0'>";
      cdata = cdata + "<tr><td><b>Cursor:</b></td></tr>";
      
      // cursor times 
      cdata = cdata + td_l + IGCview.format_clock(time1)+ td_end;
      cdata = cdata + td_r + IGCview.format_clock(time2)+ td_end;
      cdata = cdata + "<tr><td align='right'>(" + IGCview.format_time(time_diff)+ ")" + td_end;

      // cursor altitudes
      alt_units = "m";
      if (IGCview.config.convert_altitude==(float)1.0) alt_units = "ft";
      cdata = cdata + td_l + IGCview.places(alt1*IGCview.config.convert_altitude,0) + alt_units + td_end;
      cdata = cdata + td_r + IGCview.places(alt2*IGCview.config.convert_altitude,0) + alt_units + td_end;
      cdata = cdata + "<tr><td align='right'>(" + IGCview.places(alt_diff*IGCview.config.convert_altitude,0) + alt_units + ")" + td_end;

      // cursor task distances
      cdata = cdata + td_l + IGCview.places(task_dist1*IGCview.config.convert_task_dist,2)+ dist_units + td_end;
      cdata = cdata + td_r + IGCview.places(task_dist2*IGCview.config.convert_task_dist,2) + dist_units + td_end;
      cdata = cdata + "<tr><td align='right'>(" + IGCview.places(dist*IGCview.config.convert_task_dist,1) + dist_units + ")" + td_end;

      // cursor speeds in kts and kmh
      cdata = cdata + "<tr><td>" + IGCview.places(speed_knots,1) + "kts"+ td_end;
      cdata = cdata + "<tr><td>" + IGCview.places(speed_knots*IGCview.NMKM,1) + "kmh" + td_end;
       
      // climb rate between cursor points
      climb_units = "m/s";
      if (IGCview.config.convert_climb==(float)1.0) climb_units = "kt";
      cdata = cdata + "<tr><td>" + IGCview.places(climb*IGCview.config.convert_climb,1) + climb_units + td_end;

      // LD ratio between cursor points
      cdata = cdata + "<tr><td>" + IGCview.places(l_d,0) + ":1" + td_end;
      
      // table end
    	cdata = cdata + "</table>";
    	cdata = cdata + "<HR>";
      cdata = cdata + "<table width='100%' border='0' cellpadding='1' cellspacing='0'>";
      
      cdata = cdata + "<tr><td><b>Segment:</b></td></tr>";
      cdata = cdata + "<tr><td>" + (thermalling? "&nbsp;Thermalling" : "&nbsp;Cruising") + td_end;

      // segment times 
      cdata = cdata + td_l + IGCview.format_clock(mode_time1)+ td_end;
      cdata = cdata + td_r + IGCview.format_clock(mode_time2)+ td_end;
      cdata = cdata + "<tr><td align='right'>(" + IGCview.format_time(mode_time_diff)+ ")" + td_end;

      // segment altitudes
      cdata = cdata + td_l + IGCview.places(mode_alt1*IGCview.config.convert_altitude,0) + alt_units + td_end;
      cdata = cdata + td_r + IGCview.places(mode_alt2*IGCview.config.convert_altitude,0) + alt_units + td_end;
      cdata = cdata + "<tr><td align='right'>(" + IGCview.places(mode_alt_diff*IGCview.config.convert_altitude,0) + alt_units + ")" + td_end;

    	if (!thermalling)
	      {
          cdata = cdata + "<tr><td>" + IGCview.places(mode_dist*IGCview.config.convert_task_dist,1) + dist_units + td_end;
          cdata = cdata + "<tr><td>" + IGCview.places(mode_speed_knots,1) + "kts" + td_end;
          cdata = cdata + "<tr><td>" + IGCview.places(mode_speed_knots*IGCview.NMKM,1) + "kmh" + td_end;
          cdata = cdata + "<tr><td>" + IGCview.places(mode_climb*IGCview.config.convert_climb,1) + climb_units + td_end;
          cdata = cdata + "<tr><td>" + IGCview.places(mode_l_d,0) + ":1" + td_end;
	      }
      else
        cdata = cdata + "<tr><td>" + IGCview.places(mode_climb*IGCview.config.convert_climb,1) + climb_units + td_end;

      // table/page end
    	cdata = cdata + "</table></body></html>";

	IGCview.show_cursor_data(cdata); // write cursor data to browser window
    }

  static void calc_values()
    {
      if (IGCview.primary_index==0)
        {
          IGCview.msgbox("No logs loaded.");
          return;
        }
      TrackLog l = IGCview.logs[IGCview.primary_index];
      int i1 = IGCview.cursor1, i2 = IGCview.cursor2, mode_i1=1, mode_i2=1;
      int t;  // thermal counter

      thermalling = false;
      if (l.gps_ok)
        {
          for (t=1; t<=l.thermal_count; t++)
            if (l.thermal[t].finish_index>=i2)
              {
                thermalling = true;  // inside thermal[t]
                break;
              }
            else if (t==l.thermal_count) break;
            else if (t<l.thermal_count && l.thermal[t+1].start_index>i2) break;
          if (thermalling)
            {
              mode_i1 = l.thermal[t].start_index;
              mode_i2 = l.thermal[t].finish_index;
            }
          else if (l.thermal_count==0) // CRUISING (no thermals)
            {
              mode_i1 = 1;
              mode_i2 = l.record_count;
            }
          else                         // CRUISING (between thermals)
            {
	      mode_i1 = l.thermal[t].finish_index;
              if (IGCview.task==null)     // NO TASK
                {
                  if (t==l.thermal_count) // last thermal in log, no task
                    {
                      mode_i2 = l.record_count;
                      while (l.latitude[mode_i2]==(float)0.0 && --mode_i2>mode_i1) {;}
                    }
                  else mode_i2 = l.thermal[t+1].start_index;
                }
              else // HAVE TASK
                {
	          if (t==l.thermal_count)  // if cruising after last thermal
                     {
                       if (l.tps_rounded==IGCview.task.tp_count)
                         mode_i2 = l.tp_index[IGCview.task.tp_count]; // prune section to finish
                       else
                         {
                            mode_i2 = l.record_count;
                            while (l.latitude[mode_i2]==(float)0.0 && --mode_i2>mode_i1) {;}
                          }
		     }
                   else if (l.tps_rounded==IGCview.task.tp_count &&
                            l.thermal[t+1].start_index > l.tp_index[IGCview.task.tp_count])
                           mode_i2 = l.tp_index[IGCview.task.tp_count]; // prune section to finish
                        else mode_i2 = l.thermal[t+1].start_index;
                }
	    }
          mode_time1 = l.time[mode_i1];
          mode_time2 = l.time[mode_i2];
          mode_time_diff = mode_time2 - mode_time1;
          mode_alt1 = l.altitude[mode_i1];
          mode_alt2 = l.altitude[mode_i2];
          mode_alt_diff = mode_alt2 - mode_alt1;
          mode_dist = IGCview.dec_to_dist(l.latitude[mode_i1], l.longitude[mode_i1],
		    		          l.latitude[mode_i2], l.longitude[mode_i2]);
          if (mode_i1==mode_i2)
	    {
	      mode_climb = (float)0.0;
	      mode_speed_knots = (float)0.0;
	      mode_l_d = (float)0.0;
 	    }
          else
	    {
              mode_climb = mode_alt_diff / mode_time_diff * (float) 0.6;
              mode_speed_knots = mode_dist / mode_time_diff * 3600;
              if (mode_alt1==mode_alt2)
                mode_l_d = (float)0.0;
              else
                mode_l_d = -mode_dist * 6080 / mode_alt_diff;
	    }
        }
      task_dist1 = l.index_to_dist(i1);
      task_dist2 = l.index_to_dist(i2);
      time1 = l.time[i1];
      time2 = l.time[i2];
      time_diff = (time1>time2) ? time1-time2 : time2-time1;
      alt1 = l.altitude[i1];
      alt2 = l.altitude[i2];
      alt_diff = alt2-alt1;
      dist = IGCview.dec_to_dist(l.latitude[i1], l.longitude[i1],
				 l.latitude[i2], l.longitude[i2]);
      if (i1==i2)
	{
	  climb = (float)0.0;
	  speed_knots = (float)0.0;
	  l_d = (float)0.0;
	}
      else
	{
          climb = alt_diff / time_diff * (float) 0.6;
          speed_knots = dist / time_diff * 3600;
          if (alt1==alt2)
            l_d = (float)0.0;
          else
            l_d = -dist * 6080 / alt_diff;
	}
    }

}

//***************************************************************************//
//            RulerWindow                                                    //
//***************************************************************************//

class RulerWindow
{
  static float latitude=(float)0.0, longitude=(float)0.0, dist=(float)0.0, track=(float)0.0;

  RulerWindow() {;}

  static void update()
    {
      String dist_units = " km";
      if (IGCview.config.convert_task_dist==(float)1.0) dist_units = " n.miles";
      else if (IGCview.config.convert_task_dist==IGCview.NMMI) dist_units = " miles";

      String s = "<html><head><title>IGCview ruler data</title></head><body>\n";
      s = s + "<font face=\"Helvetica\">\n";
      s = s + "<em>Ruler</em><br>";

      s = s + IGCview.format_lat(latitude) + "<br>";
      s = s + IGCview.format_long(longitude) + "<br>";
      s = s + IGCview.places(dist*IGCview.config.convert_task_dist,3)+dist_units + "<br>";
      s = s + IGCview.places(track,1)+" deg" + "<br>";

      s = s + "</font></body></html>";

      IGCview.show_ruler_data(s);
    }
}

//***************************************************************************//
//            DataWindow                                                     //
//***************************************************************************//


class DataWindow
{

  static String sdata = null;
  
  DataWindow() {;}

  static void write(String s) { sdata = sdata + s; }

  static void writeln(String s) { sdata = sdata + s + "\n"; }

  public static void show_data()
    {
      TrackLog log = null;
      TrackLogS s = null;
      int i;
      int leg;
      String units="error", d_units="error"; // general units variable, and task distance units

      if (IGCview.primary_index==0)
        {
          IGCview.msgbox("No logs loaded.");
          return;
        }
      log = IGCview.logs[IGCview.primary_index];
      s = IGCview.sec_log;
      if (IGCview.task==null)
        {
          IGCview.msgbox("A task must be defined for Flight Data.");
          return;
        }
      else if (!log.gps_ok)
        {
          IGCview.msgbox("Primary log has no GPS data (baro only?).");
          return;
        }
      log.calc_flight_data();
      IGCview.sec_log.calc_flight_data();

      sdata = "<html>\n";
      writeln("<head><title>Flight Data: "+IGCview.make_title(log)+"</title></head>");
      writeln("<body><font face=\"Helvetica\">");
      writeln("<h3>Flight Data "+IGCview.make_title(log)+"</h2>");

      // Header summary

      d_units = IGCview.config.convert_task_dist==IGCview.NMKM ? " km" :
	               (IGCview.config.convert_task_dist==(float)1.0 ? " nm" : " miles");
      if (!IGCview.task.is_AAT() || log.tps_rounded==IGCview.task.tp_count)
        { // write task length if not an area task (AAT), or if task completed 
          write("<p>Task length: "+
            IGCview.places(IGCview.task.length*IGCview.config.convert_task_dist,2) + d_units + ".  ");
        }
      if (log.tps_rounded==IGCview.task.tp_count)
        {
          units = IGCview.config.convert_task_dist==IGCview.NMKM ? " km/h" :
  	              (IGCview.config.convert_task_dist==(float)1.0 ? " knots" : " mph");
          write("<strong><font color=\"green\">"+
                log.name+" Task Completed OK, "+
                IGCview.places(log.task_distance*IGCview.config.convert_task_dist,2) + d_units +
                " at "+
                IGCview.places(log.task_speed*IGCview.config.convert_task_dist,2)+units+
                "</font></strong> ("+
                IGCview.places(s.task_distance*IGCview.config.convert_task_dist,2) + d_units +
                " at "+
                IGCview.places(s.task_speed*IGCview.config.convert_task_dist,2)+units+").");
        }
      else
        {
          write(log.name+" LANDED OUT.  ");
          writeln("Achieved distance: "+
            IGCview.places(log.task_distance*IGCview.config.convert_task_dist,2)+d_units+" ("+
            IGCview.places(s.task_distance*IGCview.config.convert_task_dist,2)+d_units+").");
        }
      writeln("</p>");

      // Flight Data Table

      writeln("<table border=\"2\" cellpadding=\"6\">");
      int col_span = 2 * IGCview.task.tp_count + 1;
      int col_width = 100 / col_span;
      writeln("<colgroup span=\""+col_span+"\" width=\""+col_width+"%\">");
      writeln("</colgroup>");

      // TP column headers

      write("<tr><th></th>");

      for (i=1;i<=IGCview.task.tp_count; i++)
        {
          write("<th colspan=\"2\">");
          write(IGCview.task.tp[i].trigraph+"<br/><small>"+IGCview.trim_text(IGCview.task.tp[i].full_name)+"</small>");
          if (i>log.tps_rounded) write("<br/><small>(not reached)</small>");
          write("</th>");
        }
      writeln("</tr>");

      // Time:

      write("<tr><td><b>Time:</b></td>");
      for (i=1; i<=IGCview.task.tp_count; i++)
	  write("<td colspan=\"2\"><b>"+IGCview.format_clock(log.tp_time[i])+"</b> "+
              "<br/>("+IGCview.format_clock(s.tp_time[i])+")</td>");
      writeln("</tr>");

      // Height:

      write("<tr><td><b>Height:</b></td>");
      for (i=1; i<=IGCview.task.tp_count; i++)
  	  write("<td colspan=\"2\"><b>"+
                IGCview.places(log.tp_altitude[i]*IGCview.config.convert_altitude,0)+"</b> "+
              "<br/>("+IGCview.places(s.tp_altitude[i]*IGCview.config.convert_altitude,0)+")</td>");
      writeln("</tr>");

      // Duration:

      write("<tr><td colspan=\"2\"><b>Duration:</b></td>");
      for (leg=1; leg<IGCview.task.tp_count; leg++)
        write("<td colspan=\"2\"><b>"+IGCview.format_time(log.leg_time[leg])+"</b> "+
              "<br/>("+IGCview.format_time(s.leg_time[leg])+")</td>");
      writeln("<td><b>"+IGCview.format_time(log.task_time)+"</b> "+
            "<br/>("+IGCview.format_time(s.task_time)+")</td></tr>");

      // Distance:

      units = IGCview.config.convert_task_dist==IGCview.NMKM ? "(km):" :
                        (IGCview.config.convert_task_dist==(float)1.0 ? "(nm):" : "(miles):");
      write("<tr><td colspan=\"2\"><b>Distance"+units+"</b></td>");
      for (leg=1; leg<IGCview.task.tp_count; leg++)
        {
          write("<td colspan=\"2\"><b>");
          if (leg>=log.tps_rounded) write("["); // put brackets around leg length if not completed
          write(IGCview.places(log.leg_distance[leg]*IGCview.config.convert_task_dist,1));
          if (leg>=log.tps_rounded) write("]");
          write("</b> <br/>("+IGCview.places(s.leg_distance[leg]*IGCview.config.convert_task_dist,1)+")</td>");
        }
      writeln("<td><b>"+
            IGCview.places(log.task_distance*IGCview.config.convert_task_dist,2)+"</b> <br/>("+
            IGCview.places(s.task_distance*IGCview.config.convert_task_dist,2)+")</td></tr>");

      // Speed:

      units = IGCview.config.convert_task_dist==IGCview.NMKM ? "(km/h):" :
                   (IGCview.config.convert_task_dist==(float)1.0 ? "(knots):" : "(mph):");
      write("<tr><td colspan=\"2\"><b>Speed"+units+"</b></td>");
      for (leg=1; leg<IGCview.task.tp_count; leg++)
        write("<td colspan=\"2\"><b>"+
              IGCview.places(log.leg_speed[leg]*IGCview.config.convert_task_dist,1)+"</b> <br/>("+
              IGCview.places(s.leg_speed[leg]*IGCview.config.convert_task_dist,1)+")</td>");
      writeln("<td><b>"+
              IGCview.places(log.task_speed*IGCview.config.convert_task_dist,2)+"</b> <br/>("+
              IGCview.places(s.task_speed*IGCview.config.convert_task_dist,2)+")</td></tr>");

      // AvgClimb:

      units = IGCview.config.convert_climb==(float)1.0 ? "(knots)" : "(m/s)";
      write("<tr><td colspan=\"2\"><b>AvgClimb"+units+"</b></td>");
      for (leg=1; leg<IGCview.task.tp_count; leg++)
        write("<td colspan=\"2\"><b>"+
              IGCview.places(log.leg_climb_avg[leg]*IGCview.config.convert_climb,1)+"</b> <br/>("+
              IGCview.places(s.leg_climb_avg[leg]*IGCview.config.convert_climb,1)+")</td>");
      writeln("<td><b>"+
            IGCview.places(log.task_climb_avg*IGCview.config.convert_climb,1)+"</b> "+
            "<br/>("+IGCview.places(s.task_climb_avg*IGCview.config.convert_climb,1)+")</td></tr>");

      // %Climb:

      write("<tr><td colspan=\"2\"><b>%Climb:</b></td>");
      for (leg=1; leg<IGCview.task.tp_count; leg++)
        write("<td colspan=\"2\"><b>"+
              IGCview.places(log.leg_climb_percent[leg],0)+"%</b> <br/>("+
              IGCview.places(s.leg_climb_percent[leg],0)+"%)</td>");
      write("<td><b>"+
            IGCview.places(log.task_climb_percent,0)+"%</b> <br/>("+
            IGCview.places(s.task_climb_percent,0)+"%)</td></tr>");


      // #Climbs:

      write("<tr><td colspan=\"2\"><b>#Climbs:</b></td>");
      for (leg=1; leg<IGCview.task.tp_count; leg++)
        write("<td colspan=\"2\"><b>"+
              log.leg_climb_count[leg]+"</b> <br/>("+
              s.leg_climb_count[leg]+")</td>");
      write("<td><b>"+
            log.task_climb_count+"</b> <br/>("+
            s.task_climb_count+")</td></tr>");

      // AvgCruise:

      units = IGCview.config.convert_speed==IGCview.NMKM ? "(km/h)" :
                 (IGCview.config.convert_speed==(float)1.0 ? "(knots)" : "(mph)");
      write("<tr><td colspan=\"2\"><b>AvgCruise"+units+"</b></td>");
      for (leg=1; leg<IGCview.task.tp_count; leg++)
        write("<td colspan=\"2\"><b>"+
              IGCview.places(log.leg_cruise_avg[leg]*IGCview.config.convert_speed,1)+"</b> <br/>("+
              IGCview.places(s.leg_cruise_avg[leg]*IGCview.config.convert_speed,1)+")</td>");
      write("<td><b>"+
            IGCview.places(log.task_cruise_avg*IGCview.config.convert_speed,1)+"</b> <br/>("+
            IGCview.places(s.task_cruise_avg*IGCview.config.convert_speed,1)+")</td></tr>");

      writeln("</table>");

      writeln("</font>");
      writeln("</body>");
      writeln("</html>");

      IGCview.show_flight_data(sdata);
    }

}

//***************************************************************************//
//            MaggotRacer                                                    //
//***************************************************************************//

// The MaggotRacer thread updates the map screen every RACE_PAUSE milliseconds, stepping through
// each selected logtrace to the next point after the current time.  The current time is incremented
// by RACE_TIME_INCREMENT on each step.
// 'race_faster()' and 'race_slower()' control the speed of the replay by manipulating
// the 'race_pause' and 'race_step' variables.

class MaggotRacer extends Thread
{
  static Graphics g;
  static boolean synchro_start;
  static RacingCanvas c;
  static int [] time_offset = new int [IGCview.MAXLOGS+1]; // time offsets for synchro
  int first_log_start=90000, first_task_start=90000;
  static int [] index = new int [IGCview.MAXLOGS+1]; // index of current point in log
  boolean pause = false;  // flag set to true when race should pause
  long race_pause = IGCview.config.RACEPAUSE;      // number of milliseconds pause between each update
  int race_step = IGCview.RACE_TIME_INCREMENT;     // number of seconds jump between log points

  MaggotRacer(Graphics g, RacingCanvas c, boolean synchro_start)
    {
      int i;
      TrackLog log;
      this.c = c;
      this.g = g;
      this.synchro_start = synchro_start;
      for (i=1; i<=IGCview.log_count; i++)
	{
	  if (i==IGCview.primary_index || IGCview.secondary[i])
	      {
	    	log = IGCview.logs[i];
	        time_offset[i] = 0;
		index[i]=1;
	    	if (log.time[1]<first_log_start) first_log_start = log.time[1];
	    	if (log.tps_rounded>0 && log.tp_time[1]<first_task_start)
		  first_task_start = log.tp_time[1];
	      }
	}
      // c.draw_legend(g);
      if (synchro_start)
	for (i=1; i<=IGCview.log_count; i++)
	  {
	    if (i==IGCview.primary_index || IGCview.secondary[i])
	      {
		log = IGCview.logs[i];
		if (log.tps_rounded>0) time_offset[i] = log.tp_time[1] - first_task_start;
              }
          }
    }

  public void run()
    {
      if (synchro_start)
	  race_loop(first_task_start - 1200);  // start maggot race start - 20mins
	else
	  race_loop(first_log_start);
    }

  void race_loop(int start_time)
    {
      int i;
      int time, finish_time = 90000;
      boolean cont;               // continue within one race flag
      boolean repeat_loop = true; // continue looping through race
      TrackLog log;

      while (repeat_loop)
	  {
          time = start_time;
          cont = true;
          for (i=1; i<=IGCview.log_count; i++)
	      if (i==IGCview.primary_index || IGCview.secondary[i]) index[i]=1;
	    while (cont)
	    {
	      cont = false;
	      for (i=1; i<=IGCview.log_count; i++)
	  	{
	     	  if (i==IGCview.primary_index || IGCview.secondary[i])
		    {
			c.draw_plane(g,i,index[i]); // erase old plane
		  	log = IGCview.logs[i];
                  if (race_step>0)
			  while (index[i]<log.record_count &&
                           log.time[index[i]]-time_offset[i]<=time) index[i]++;
                  else /* race_step<0 */
			  while (index[i]>1 &&
                           log.time[index[i]]-time_offset[i]>=time) index[i]--;
			if ((race_step>0 && index[i]<log.record_count) ||
                      (race_step<0 && index[i]>1)) cont = true;
			c.draw_plane(g,i,index[i]); // draw new plane
		    }
		}
  	      time += race_step;
	      c.mark_time(g,time); // write time
	      try {
                  sleep(race_pause);
                  while (pause) sleep(1000);
	          c.mark_time(g,time); // erase time
                }
            catch (InterruptedException e){ repeat_loop = false; cont = false; }
	    }
        }
    }

  void race_pause()
    {
       pause = !pause; // toggle pause
    }

  void race_continue()
    {
       pause = false;
    }

  void race_forwards()
    {
      if (pause || race_step < 0)  // if paused or racing backwards, then run forwards at initial speed
	{
          pause = false;
          race_pause = IGCview.config.RACEPAUSE;      // number of milliseconds pause between each update
          race_step = IGCview.RACE_TIME_INCREMENT;     // number of seconds jump between log points
        }
      else
	race_faster();
    }
 
  void race_backwards()
    {
      if (pause || race_step > 0)  // if paused or racing forwards, then run backwards at initial speed
	{
          pause = false;
          race_pause = IGCview.config.RACEPAUSE;      // number of milliseconds pause between each update
          race_step = -IGCview.RACE_TIME_INCREMENT;     // number of seconds jump between log points
        }
      else
	race_faster();
    }

  void race_faster()
    {
      // if we're currently pausing less than 10ms between updates, then increase the speed by
      // increasing the 'race_step' time jump between updates.  Otherwise simply halve the 'race_pause'.
      if (race_pause < 10) race_step *=2;
      else race_pause /= 2;
    }

  void race_slower()
    {
      // if the current tracklog time step beween each update is greater than 4 seconds, then
      // slow the replay by halving that timestep (doubling the 'detail' of the replay).  Otherwise
      // simply double the 'race_pause', so the screen updates are half as frequent.
      if ( race_step > 4 ||
           race_step < -4) race_step /= 2;
      else if (race_pause < 5000) race_pause *= 2;
    }
}

//***************************************************************************//
//             ConfigFrame                                                   //
//***************************************************************************//

class ConfigFrame extends Frame implements WindowListener
{

  ConfigWindow window;

  private static String [] [] menus = {
    {"File", "Use these settings", "Reset Defaults", "Cancel and Close"}
  };    

  private int width, height;

  ConfigFrame ()
    {
      setTitle("Preferences"); 
      width = IGCview.config.CONFIGWIDTH;
      height = IGCview.config.CONFIGHEIGHT;
      setSize(width, height);
      this.addWindowListener(this);
      ScrollPane pane = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
      this.add (pane, "Center");
      window = new ConfigWindow(this);
      pane.add(window);

      MenuBar menubar = new MenuBar ();
      this.setMenuBar (menubar);

      for (int c = 0; c < menus.length; c++) {
        Menu m = new Menu (menus [c][0]);
        menubar.add (m);
        for (int r = 1; r < menus [c] .length; r++) {
  	  if (menus [c][r] == null)
              m.addSeparator ();
	  else
            {
	      MenuItem i = new MenuItem (menus [c][r]);
	      m.add (i);
	      i.setActionCommand (menus [c][r] .toLowerCase ());
	      i.addActionListener (window);
  	    }
        }
      }
      this.pack ();
    }

  public Dimension getPreferredSize ()
    {
      return new Dimension (width, height);
    }

  // WindowListener interface:
  public void windowActivated(WindowEvent e) {;}
  public void windowClosed(WindowEvent e) {;}
  public void windowClosing(WindowEvent e)
    {
      dispose();
	IGCview.config_window = null;
    }

  public void windowDeactivated(WindowEvent e) {;}
  public void windowDeiconified(WindowEvent e) {;}
  public void windowIconified(WindowEvent e) {;}
  public void windowOpened(WindowEvent e) {;}

}

//***************************************************************************//
//            ConfigWindow                                                   //
//***************************************************************************//

class ConfigWindow extends Panel implements ActionListener
{
  // MAP CONFIG
  private Checkbox draw_pri_thermals = new Checkbox("Box around primary thermals");
  private Checkbox draw_sec_thermals = new Checkbox("Box around secondary thermals");
  private Checkbox draw_pri_thermal_ends = new Checkbox("Mark thermal entry/exit");
  private Checkbox draw_tp_times = new Checkbox("Mark sector entry");
  private Checkbox draw_tps = new Checkbox("Mark turnpoints on map");
  private Checkbox draw_tp_names = new Checkbox("Display TP names");
  private Checkbox draw_sectors = new Checkbox("Display turnpoint sectors on map");
  private Checkbox trim_logs = new Checkbox("Trim track log before start/after finish");
  
  private TextField start_xscale = new TextField(String.valueOf(IGCview.canvas.zoom.xscale));

  private TextField start_latitude = new TextField(String.valueOf(IGCview.canvas.zoom.latitude));

  private TextField start_longitude = new TextField(String.valueOf(IGCview.canvas.zoom.longitude));

  private Checkbox beer_can_sectors = new Checkbox("Beer-can TP sectors");
  
  private Checkbox photo_sectors = new Checkbox("Photo TP sectors");
  
  private TextField beer_tp_radius = new TextField(IGCview.places(IGCview.config.beer_tp_radius*
                                                             IGCview.NMKM * 1000, 0));

  private TextField photo_tp_radius = new TextField(IGCview.places(IGCview.config.photo_tp_radius*
                                                             IGCview.NMKM * 1000, 0));

  private Checkbox startline_180 = new Checkbox("Perpendicular start line (vs photo sector)");
  
  private TextField startline_radius =
            new TextField(IGCview.places(IGCview.config.startline_radius*IGCview.NMKM, 1));

  private TextField time_offset = new TextField(String.valueOf(IGCview.config.time_offset));

  private Checkbox auto_zoom = new Checkbox("Auto zoom-to-flight on track load");

  // WINDOWS
  private TextField SELECTWIDTH = new TextField(String.valueOf(
				   (IGCview.select_window==null) ? IGCview.config.SELECTWIDTH :
                                                                IGCview.select_window.getSize().width));
  private TextField SELECTHEIGHT = new TextField(String.valueOf(
				   (IGCview.select_window==null) ? IGCview.config.SELECTHEIGHT :
                                                                IGCview.select_window.getSize().height));

  // COLOURS
  private ColourChoice MAPBACKGROUND = new ColourChoice(IGCview.config.MAPBACKGROUND, "Map background");
  private ColourChoice CURSORCOLOUR = new ColourChoice(IGCview.config.CURSORCOLOUR, "Map cross-hair cursors");
  private ColourChoice SECAVGCOLOUR = new ColourChoice(IGCview.config.SECAVGCOLOUR,
                                                       "Secondary in Climb Profile");
  private ColourChoice ALTCOLOUR = new ColourChoice(IGCview.config.ALTCOLOUR,
						    "Primary in Altitude window");
  private ColourChoice ALTSECCOLOUR = new ColourChoice(IGCview.config.ALTSECCOLOUR, 
						       "Secondary in Altitude window");
  private ColourChoice TPBARCOLOUR = new ColourChoice(IGCview.config.TPBARCOLOUR, 
						   "Vertical lines in Alt/Climb/Cruise windows marking TPs");
  private ColourChoice TASKCOLOUR = new ColourChoice(IGCview.config.TASKCOLOUR, 
						     "Task drawn on map");
  private ColourChoice TPTEXT = new ColourChoice(IGCview.config.TPTEXT, 
						     "Text color for TP labels");
  private ColourChoice TPCOLOUR = new ColourChoice(IGCview.config.TPCOLOUR, 
						     "Color for TP's on map");
  private ColourChoice CLIMBCOLOUR = new ColourChoice(IGCview.config.CLIMBCOLOUR, 
                                                      "Primary climb bars in Climb window");
  private ColourChoice CLIMBSECCOLOUR = new ColourChoice(IGCview.config.CLIMBSECCOLOUR,
                                                         "Secondary climb bars in Climb window");
  private ColourChoice CRUISECOLOUR = new ColourChoice(IGCview.config.CRUISECOLOUR, 
                                                       "Primary cruise bars in Cruise window");
  private ColourChoice CRUISESECCOLOUR = new ColourChoice(IGCview.config.CRUISESECCOLOUR,
                                                          "Secondary cruise bars in Cruise window");

  // FILES
//  private Checkbox startup_load_tps = new Checkbox("Load tp file at startup");
  private TextField TPFILE = new TextField(String.valueOf(IGCview.config.TPFILE));
  private TextField LOGDIR = new TextField(String.valueOf(IGCview.config.LOGDIR));

  // MAGGOT RACE PARAMETERS
  private TextField RACEPAUSE = new TextField(String.valueOf(IGCview.config.RACEPAUSE));
  private Checkbox altsticks = new Checkbox("Draw altitude sticks in Replay (default BELOW)");
  private Checkbox upside_down_lollipops = new Checkbox("Altitude stick position ABOVE planes");
  private Checkbox glider_icons = new Checkbox("Glider icons in Replay (default LOLLIPOPS)");

  // UNIT CONVERSIONS
  // task distance units
  private CheckboxGroup convert_task_dist_group = new CheckboxGroup();
  private Checkbox convert_task_dist_km = new Checkbox("km",
                                                       IGCview.config.convert_task_dist==IGCview.NMKM,
                                                       convert_task_dist_group);
  private Checkbox convert_task_dist_nm = new Checkbox("nm",
                                                       IGCview.config.convert_task_dist==(float) 1.0,
                                                       convert_task_dist_group);
  private Checkbox convert_task_dist_mi = new Checkbox("mi",
                                                       IGCview.config.convert_task_dist==IGCview.NMMI,
                                                       convert_task_dist_group);
  // speed units
  private CheckboxGroup convert_speed_group = new CheckboxGroup();
  private Checkbox convert_speed_kmh = new Checkbox("kmh",
                                                       IGCview.config.convert_speed==IGCview.NMKM,
                                                       convert_speed_group);
  private Checkbox convert_speed_kts = new Checkbox("kts",
                                                       IGCview.config.convert_speed==(float) 1.0,
                                                       convert_speed_group);
  private Checkbox convert_speed_mph = new Checkbox("mph",
                                                       IGCview.config.convert_speed==IGCview.NMMI,
                                                       convert_speed_group);
  // altitude units
  private CheckboxGroup convert_altitude_group = new CheckboxGroup();
  private Checkbox convert_altitude_ft = new Checkbox("ft",
                                                       IGCview.config.convert_altitude==(float)1.0,
                                                       convert_altitude_group);
  private Checkbox convert_altitude_m = new Checkbox("m",
                                                       IGCview.config.convert_altitude!=(float) 1.0,
                                                       convert_altitude_group);
  // climb units
  private CheckboxGroup convert_climb_group = new CheckboxGroup();
  private Checkbox convert_climb_kts = new Checkbox("kts",
                                                       IGCview.config.convert_climb==(float)1.0,
                                                       convert_climb_group);
  private Checkbox convert_climb_ms = new Checkbox("m/s",
                                                       IGCview.config.convert_climb!=(float) 1.0,
                                                       convert_climb_group);

  private ConfigFrame config_frame;              // frame to draw on

  GridBagLayout gridbag;
  GridBagConstraints c;

  void set_text(TextField t, String s)
    {
      c.gridx = 0;
      c.gridwidth = 4;
      t.setColumns(25);
      Label l = new Label(s);
      //c.gridwidth = GridBagConstraints.RELATIVE;
      gridbag.setConstraints(t, c);
      t.addActionListener(this);
      //t.addFocusListener(this);
      add(t);
      c.gridx = GridBagConstraints.RELATIVE;
      c.gridwidth = GridBagConstraints.REMAINDER;
      gridbag.setConstraints(l,c);
      add(l);
    }

  void set_checkbox(Checkbox cb)
    {
      c.gridx = 3;
      c.gridwidth = GridBagConstraints.REMAINDER;
      gridbag.setConstraints(cb, c);
      add(cb);
    }

  void set_new_line()
    {
      Label l = new Label("");
      c.gridwidth = GridBagConstraints.REMAINDER;
      gridbag.setConstraints(l,c);
      add(l);
    }

  void set_colour_headings()
    {
      Label l;
      l = new Label("Light");
      c.gridx = 1;
      c.gridwidth = 1;
      gridbag.setConstraints(l, c);
      add(l);
      l = new Label("Dark");
      c.gridx = 3;
      c.gridwidth = GridBagConstraints.REMAINDER;
      gridbag.setConstraints(l,c);
      add(l);
    }

  void set_text_line(String s)
    {
      Label l = new Label(s);
      c.gridx = 1;
      c.gridwidth = GridBagConstraints.REMAINDER;
      gridbag.setConstraints(l,c);
      add(l);
    }

  void set_convert_task_dist()
    {
      Label l = new Label("Task distance units");

      c.gridx = 1;
      c.gridwidth = 1;
      gridbag.setConstraints(convert_task_dist_km, c);
      add(convert_task_dist_km);
      c.gridx = GridBagConstraints.RELATIVE;
      gridbag.setConstraints(convert_task_dist_nm, c);
      add(convert_task_dist_nm);
      gridbag.setConstraints(convert_task_dist_mi, c);
      add(convert_task_dist_mi);
      c.gridwidth = GridBagConstraints.REMAINDER;
      gridbag.setConstraints(l,c);
      add(l);
    }


  void set_convert_speed()
    {
      Label l = new Label("Speed units");

      c.gridx = 1;
      c.gridwidth = 1;
      gridbag.setConstraints(convert_speed_kmh, c);
      add(convert_speed_kmh);
      c.gridx = GridBagConstraints.RELATIVE;
      gridbag.setConstraints(convert_speed_kts, c);
      add(convert_speed_kts);
      gridbag.setConstraints(convert_speed_mph, c);
      add(convert_speed_mph);
      c.gridwidth = GridBagConstraints.REMAINDER;
      gridbag.setConstraints(l,c);
      add(l);
    }

  void set_convert_altitude()
    {
      Label l = new Label("Height units");

      c.gridx = 2;
      c.gridwidth = 1;
      gridbag.setConstraints(convert_altitude_ft, c);
      add(convert_altitude_ft);
      c.gridx = GridBagConstraints.RELATIVE;
      gridbag.setConstraints(convert_altitude_m, c);
      add(convert_altitude_m);
      c.gridwidth = GridBagConstraints.REMAINDER;
      gridbag.setConstraints(l,c);
      add(l);
    }

  void set_convert_climb()
    {
      Label l = new Label("Climb units");

      c.gridx = 2;
      c.gridwidth = 1;
      gridbag.setConstraints(convert_climb_kts, c);
      add(convert_climb_kts);
      c.gridx = GridBagConstraints.RELATIVE;
      gridbag.setConstraints(convert_climb_ms, c);
      add(convert_climb_ms);
      c.gridwidth = GridBagConstraints.REMAINDER;
      gridbag.setConstraints(l,c);
      add(l);
    }

  ConfigWindow(ConfigFrame config_frame)
    {
      this.config_frame = config_frame;

      gridbag = new GridBagLayout();
      c = new GridBagConstraints();
 
      setFont(new Font("Helvetica", Font.PLAIN, 12));
      setLayout(gridbag);
   
      draw();
    }

  void draw()
    {
      gridbag.invalidateLayout(this);
      c.insets = new Insets(3,3,3,3);                 // space 3 pixels around fields
      c.anchor = GridBagConstraints.WEST;

      c.weightx = 0.0;

      set_text_line("Set your preferences for IGCview using this window.");
      set_new_line();
      set_text_line("LATITUDE, LONGITUDE, SCALE");
      set_text_line("The easiest way to set latitude, longitude and scale is to position and scale the");      
      set_text_line("map window *before* launching this prefences window.");      
      set_text(start_latitude, "Startup latitude e.g. 53.5 = 53 deg 30 mins N");
      set_text(start_longitude, "Startup longitude e.g. -1.1 = 1 deg 6 mins W");
      set_text(start_xscale, "Startup scale (pixels/degree) e.g. 200");

      set_text_line("LOCAL TIME ZONE");
      set_text(time_offset, "Local time offset from UTC");
      
      set_text_line("TURNPOINT DATABASE AND IGC FILE DIRECTORY");
      set_text_line("You can set your turnpoint database file to a file on your PC, e.g.");
      set_text_line("turnpoints/bga.gdn");
      set_text_line("This file is relative to the IGCview directory.");
      set_text_line("Or you can also point directly at a TP file on the internet, e.g.");
      set_text_line("http://acro.harvard.edu/SOARING/JL/TP/Mifflin/mf02_gdn.wpt");
      set_text(TPFILE, "Turnpoint file, e.g. bga.gdn");
      set_text(LOGDIR, "Log file directory, e.g. c:\\logfiles\\");

      set_new_line();
      set_text_line("DISTANCE, SPEED, ALTITUDE, CLIMB UNITS");
      set_convert_task_dist();
      set_convert_speed();
      set_convert_altitude();
      set_convert_climb();
      set_new_line();

      set_text_line("PREFERENCES BELOW FOR THIS SESSION ONLY");
      set_text_line("Note that the prefences below are not saved after you close IGCview.");
      
      set_checkbox(auto_zoom);
      auto_zoom.setState(IGCview.config.auto_zoom);

      set_checkbox(draw_tps);
      draw_tps.setState(IGCview.config.draw_tps);
      set_checkbox(draw_tp_names);
      draw_tp_names.setState(IGCview.config.draw_tp_names);
      set_checkbox(draw_sectors);
      draw_sectors.setState(IGCview.config.draw_sectors);
      set_checkbox(draw_pri_thermals);
      draw_pri_thermals.setState(IGCview.config.draw_pri_thermals);
      set_checkbox(draw_sec_thermals);
      draw_sec_thermals.setState(IGCview.config.draw_sec_thermals);
      set_checkbox(draw_pri_thermal_ends);
      draw_pri_thermal_ends.setState(IGCview.config.draw_pri_thermal_ends);
      set_checkbox(draw_tp_times);
      draw_tp_times.setState(IGCview.config.draw_tp_times);
      set_checkbox(altsticks);
      altsticks.setState(IGCview.config.altsticks);
      set_checkbox(upside_down_lollipops);
      upside_down_lollipops.setState(IGCview.config.upside_down_lollipops);
      set_checkbox(glider_icons);
      glider_icons.setState(IGCview.config.glider_icons);
      set_checkbox(trim_logs);
      trim_logs.setState(IGCview.config.trim_logs);

      set_checkbox(beer_can_sectors);
      beer_can_sectors.setState(IGCview.config.beer_can_sectors);
      set_text(beer_tp_radius, "Radius of TP sector (meters)");

      set_checkbox(photo_sectors);
      photo_sectors.setState(IGCview.config.photo_sectors);
      set_text(photo_tp_radius, "Radius of photo TP sector (meters)");

      set_checkbox(startline_180);
      startline_180.setState(IGCview.config.startline_180);
      set_text(startline_radius, "Radius of start line (km)");

//      set_text(SELECTWIDTH, "Width of track select window (e.g. 400 pixels)");
//      set_text(SELECTHEIGHT, "Height of track select window (e.g. 300 pixels)");
//      set_checkbox(startup_load_tps);
//      startup_load_tps.setState(IGCview.config.startup_load_tps);
//      set_text(RACEPAUSE, "Pause (milliseconds) between maggot race updates e.g. 100");

      set_new_line();
      set_colour_headings();
      MAPBACKGROUND.draw(this);
      CURSORCOLOUR.draw(this);
      SECAVGCOLOUR.draw(this);
      ALTCOLOUR.draw(this);
      ALTSECCOLOUR.draw(this);
      TPBARCOLOUR.draw(this);
      TASKCOLOUR.draw(this);
      TPCOLOUR.draw(this);
      TPTEXT.draw(this);
      CLIMBCOLOUR.draw(this);
      CLIMBSECCOLOUR.draw(this);
      CRUISECOLOUR.draw(this);
      CRUISESECCOLOUR.draw(this);
    }

  public void actionPerformed(ActionEvent e)
    {
      String label = e.getActionCommand();
      if (label.equals("use these settings")) use_config();
      else if (label.equals("reset defaults")) reset_config();
      else if (label.equals("cancel and close"))
        { config_frame.dispose();
          IGCview.config_window = null;
        }
    }

  void use_config()
    { 
      IGCview.config.draw_pri_thermals = draw_pri_thermals.getState();
      IGCview.config.draw_sec_thermals = draw_sec_thermals.getState();
      IGCview.config.draw_pri_thermal_ends = draw_pri_thermal_ends.getState();
      IGCview.config.draw_tp_times = draw_tp_times.getState();
      IGCview.config.draw_tps = draw_tps.getState();
      IGCview.config.draw_tp_names = draw_tp_names.getState();
      IGCview.config.draw_sectors = draw_sectors.getState();
      IGCview.config.altsticks = altsticks.getState();
      IGCview.config.upside_down_lollipops = upside_down_lollipops.getState();
      IGCview.config.glider_icons = glider_icons.getState();
      IGCview.config.trim_logs = trim_logs.getState();
      IGCview.config.beer_can_sectors = beer_can_sectors.getState();
      IGCview.config.photo_sectors = photo_sectors.getState();
      IGCview.config.beer_tp_radius = Float.valueOf(beer_tp_radius.getText()).floatValue()/IGCview.NMKM/1000;
      IGCview.config.photo_tp_radius = Float.valueOf(photo_tp_radius.getText()).floatValue()/IGCview.NMKM/1000;
      IGCview.config.startline_180 = startline_180.getState();
      IGCview.config.startline_radius = Float.valueOf(startline_radius.getText()).floatValue()/
                                        IGCview.NMKM;
      IGCview.config.time_offset = Integer.valueOf(time_offset.getText()).intValue();
      IGCview.config.start_xscale = Float.valueOf(start_xscale.getText()).floatValue();
      IGCview.config.start_latitude = Float.valueOf(start_latitude.getText()).floatValue();
      IGCview.config.start_longitude = Float.valueOf(start_longitude.getText()).floatValue();
      // IGCview.config.SELECTWIDTH = Integer.valueOf(SELECTWIDTH.getText()).intValue();
      // IGCview.config.SELECTHEIGHT = Integer.valueOf(SELECTHEIGHT.getText()).intValue();
      // IGCview.config.startup_load_tps = startup_load_tps.getState();
      String new_tp_file = TPFILE.getText(); // if a new TP file then load it
      if (!IGCview.config.TPFILE.equals(new_tp_file))
        {
          IGCview.config.TPFILE = new_tp_file;
          IGCview.canvas.load_tps();
        }
      IGCview.config.LOGDIR = LOGDIR.getText();
      // IGCview.config.RACEPAUSE = Integer.valueOf(RACEPAUSE.getText()).intValue();
      IGCview.config.auto_zoom = auto_zoom.getState();

      IGCview.config.convert_task_dist = convert_task_dist_km.getState() ? IGCview.NMKM :
	                                 (convert_task_dist_nm.getState() ? (float) 1.0 : IGCview.NMMI);
      IGCview.config.convert_speed = convert_speed_kmh.getState() ? IGCview.NMKM :
	                                 (convert_speed_kts.getState() ? (float) 1.0 : IGCview.NMMI);
      IGCview.config.convert_altitude = convert_altitude_ft.getState() ? (float)1.0 : IGCview.FTM;
      IGCview.config.convert_climb = convert_climb_kts.getState() ? (float)1.0 : IGCview.KTMS;
      IGCview.config.MAPBACKGROUND.set(MAPBACKGROUND);
      IGCview.config.CURSORCOLOUR.set(CURSORCOLOUR);
      IGCview.config.SECAVGCOLOUR.set(SECAVGCOLOUR);
      IGCview.config.ALTCOLOUR.set(ALTCOLOUR);
      IGCview.config.ALTSECCOLOUR.set(ALTSECCOLOUR);
      IGCview.config.TPBARCOLOUR.set(TPBARCOLOUR);
      IGCview.config.TASKCOLOUR.set(TASKCOLOUR);
      IGCview.config.TPCOLOUR.set(TPCOLOUR);
      IGCview.config.TPTEXT.set(TPTEXT);
      IGCview.config.CLIMBCOLOUR.set(CLIMBCOLOUR);
      IGCview.config.CLIMBSECCOLOUR.set(CLIMBSECCOLOUR);
      IGCview.config.CRUISECOLOUR.set(CRUISECOLOUR);
      IGCview.config.CRUISESECCOLOUR.set(CRUISESECCOLOUR);

      IGCview.canvas.zoom = new Zoom(IGCview.config.start_latitude,
                                        IGCview.config.start_longitude,
                                        IGCview.config.start_xscale);
      // save (some of) configuration in cookie            
      IGCview.save_config();                                        
      IGCview.canvas.paint(IGCview.canvas.getGraphics());
      config_frame.dispose();
      IGCview.config_window = null;
    }

  private void reset_config()
    {
      IGCview.config = new Config();
      IGCview.canvas.paint(IGCview.canvas.getGraphics());
      config_frame.dispose();
      IGCview.config_window = null;
    }

}

//***************************************************************************//
//            ColourChoice                                                   //
//***************************************************************************//


class ColourChoice implements ItemListener 
{

  Choice choice; //pop-up list of choices
  ConfigColour config_colour;
  Label l;
  Checkbox [] shade_box = new Checkbox[3]; // radio buttons to select lighter/normal/darker

  ColourChoice(ConfigColour config_colour, String s)
    {
      this.config_colour = config_colour;
      l = new Label(s);
      choice = new Choice();
      choice.addItem("blue");    // 0
      choice.addItem("red");     // 1
      choice.addItem("white");   // 2
      choice.addItem("cyan");    // 3
      choice.addItem("magenta"); // 4
      choice.addItem("black");   // 5
      choice.addItem("pink");    // 6
      choice.addItem("yellow");  // 7
      choice.addItem("green");   // 8
      choice.addItem("orange");  // 9
      choice.select(config_colour.index);
      choice.addItemListener(this);
      CheckboxGroup shade_group = new CheckboxGroup();
      shade_box[0] =  new Checkbox("",(config_colour.shade==0),shade_group);
      shade_box[1] =  new Checkbox("",(config_colour.shade==1),shade_group);
      shade_box[2] =  new Checkbox("",(config_colour.shade==2),shade_group);
    }

  int selected_colour()
    {
      return choice.getSelectedIndex();
    }

  int selected_shade()
    {
      return shade_box[0].getState() ?  0 : (shade_box[1].getState() ? 1 : 2);
    }

  public void itemStateChanged(ItemEvent e) {};

  void draw(ConfigWindow w)
    {
      GridBagConstraints c = new GridBagConstraints();

      c.anchor = GridBagConstraints.WEST;
      c.gridx = GridBagConstraints.RELATIVE;
      c.gridwidth = 1;
      w.gridbag.setConstraints(choice, c);
      w.add(choice);
      c.gridx = GridBagConstraints.RELATIVE;
      w.gridbag.setConstraints(shade_box[0],c);
      w.add(shade_box[0]);
      w.gridbag.setConstraints(shade_box[1],c);
      w.add(shade_box[1]);
      c.gridwidth = GridBagConstraints.RELATIVE;
      w.gridbag.setConstraints(shade_box[2],c);
      w.add(shade_box[2]);
      c.gridwidth = GridBagConstraints.REMAINDER;
      w.gridbag.setConstraints(l,c);
      w.add(l);
    }
}

//***************************************************************************//
//             TPFrame                                                       //
//***************************************************************************//

class TPFrame extends Frame implements ActionListener, WindowListener
{
  TPCanvas [] canvas;
  int current_canvas = 1;
  boolean ruler=false;            // ruler / zoom flag

  // main MENU definitions
  private static String [] [] menus = {
    {"File", "Close"},
    {"Zoom", "Zoom In", "Zoom Out", "Zoom Reset"},
    {"Tools", "Ruler/Zoom"}
  };    

  String title = "IGCview TP detail for "+IGCview.logs[IGCview.primary_index].name+". ";

  int width, height;

  TPFrame ()
    {
      this.setTitle(title);
      this.width = IGCview.config.TPWIDTH;
      this.height = IGCview.config.TPHEIGHT;
      this.addWindowListener(this);
      canvas = new TPCanvas[IGCview.task.tp_count+1];

      ScrollPane pane = new ScrollPane ();
      Panel panel = new Panel(new GridLayout(IGCview.config.TPROWS, IGCview.config.TPCOLS));
      for (int i=1;i<=IGCview.task.tp_count;i++)
        {
          canvas[i] = new TPCanvas (this, width / 2, height / 2, i);
          panel.add (canvas[i]);
        }
      pane.add(panel);
      this.add(pane);

      MenuBar menubar = new MenuBar ();
      this.setMenuBar (menubar);

      for (int c = 0; c < menus.length; c++) {
        Menu m = new Menu (menus [c][0]);
        menubar.add (m);
        for (int r = 1; r < menus [c] .length; r++) {
  	  if (menus [c][r] == null)
              m.addSeparator ();
	  else
            {
	      MenuItem i = new MenuItem (menus [c][r]);
	      m.add (i);
	      i.setActionCommand (menus [c][r] .toLowerCase ());
	      i.addActionListener (this);
  	    }
        }
      }
      this.pack ();
    }

  public Dimension getPreferredSize ()
    {
      return new Dimension (width, height);
    }

  public void actionPerformed (ActionEvent e)
    {
      String s = e.getActionCommand ();
      if (s.equals ("zoom out"))
             { 
               cancel_ruler();
               canvas[current_canvas].zoom.zoom_out();
               canvas[current_canvas].paint(canvas[current_canvas].getGraphics());
             }
      else if (s.equals ("zoom in"))
             { 
               cancel_ruler();
               canvas[current_canvas].zoom.zoom_in();
               canvas[current_canvas].paint(canvas[current_canvas].getGraphics());
             }
      else if (s.equals ("zoom reset"))
             { 
               cancel_ruler();
               canvas[current_canvas].zoom.zoom_reset();
               canvas[current_canvas].zoom.zoom_out();
               canvas[current_canvas].paint(canvas[current_canvas].getGraphics());
             }
      else if (s.equals("ruler/zoom"))
	     {
               if (ruler) 
                 { 
                   cancel_ruler();
                 }
               else
                 {
                   ruler = true;
                 }
             }
      else if (s.equals("close"))
        { 
          if (!IGCview.canvas.ruler) cancel_ruler();
          IGCview.tp_window = null;
          dispose();
        }
    }

  void cancel_ruler()
    {
      ruler = false;
    }

  // WindowListener interface:
  public void windowActivated(WindowEvent e) {;}
  public void windowClosed(WindowEvent e) {;}
  public void windowClosing(WindowEvent e)
    {
      dispose();
	IGCview.tp_window = null;
    }

  public void windowDeactivated(WindowEvent e) {;}
  public void windowDeiconified(WindowEvent e) {;}
  public void windowIconified(WindowEvent e) {;}
  public void windowOpened(WindowEvent e) {;}

}

//***************************************************************************//
//            TPCanvas                                                       //
//***************************************************************************//

class TPCanvas extends Canvas implements MouseListener,
                                         MouseMotionListener,
                                         Drawable
{
  private TPFrame frame;
  private int width, height;
  private int tp_number;
  private TP tp;
  Zoom zoom;
  int rect_x = 0, rect_y =0;      // x,y of mouse press
  int rect_x1 = 0, rect_y1 = 0;   // new draw x,y coordinates
  int prev_rect_x1 = 0, prev_rect_y1 = 0;   // old draw x,y coordinates

  TPCanvas (TPFrame frame, int width, int height, int tp_number)
    {
      this.frame = frame;
      this.width = width;
      this.height = height;
      this.tp_number = tp_number;
      tp = IGCview.task.tp[tp_number];
      this.addMouseListener (this);
      this.addMouseMotionListener (this);
      zoom = new Zoom(IGCview.task.tp[tp_number].latitude,
                      IGCview.task.tp[tp_number].longitude,
                      IGCview.config.tp_xscale);
      zoom.zoom_out();
    }

  public Dimension getPreferredSize () 
    {
      return new Dimension (width, height);
    }

  public Dimension getMinimumSize () 
    {
      return new Dimension (100,100);
    }

  public void update (Graphics g)
    {
      g.setColor(IGCview.config.MAPBACKGROUND.colour);
      g.setXORMode(Color.white);
      if (frame.ruler && rect_x!=0)
        {
          g.drawLine(rect_x, rect_y, prev_rect_x1, prev_rect_y1);  // remove old line
          g.drawLine(rect_x, rect_y, rect_x1, rect_y1);    // draw new line
          prev_rect_x1 = rect_x1;
          prev_rect_y1 = rect_y1;  // save dimensions of drawn line
        }
      else if ((rect_x1-rect_x) > 0) 
        {
          g.drawRect(rect_x, rect_y, prev_rect_x1-rect_x, prev_rect_y1-rect_y);  // remove old rectangle
          g.drawRect(rect_x, rect_y, rect_x1-rect_x, rect_y1-rect_y);  // draw new rectangle
          prev_rect_x1 = rect_x1;
          prev_rect_y1 = rect_y1;  // save dimensions of drawn rectangle
        }
    }

  public void paint (Graphics g)
    { 
      int x, y, w ,h;

      rect_x = 0; // reset ruler/zoom box coordinates
      rect_y = 0;
      prev_rect_x1 = 0;
      prev_rect_y1 = 0;
      Dimension d = getSize();
      g.setColor(IGCview.config.MAPBACKGROUND.colour);
      g.fillRect(0,0, d.width - 1, d.height - 1);

      g.setColor(IGCview.config.TPCOLOUR.colour);
      x = long_to_x(tp.longitude);
      y = lat_to_y(tp.latitude);
      w = 6;
      h = 6;
      g.drawOval(x-3,y-3,w,h);
      g.setColor(IGCview.config.TPTEXT.colour);
      g.drawString(tp.trigraph, x+6, y);

      g.setColor(IGCview.config.TASKCOLOUR.colour);
      if (tp_number>1) IGCview.drawLineP(g,x,y,IGCview.task.track[tp_number]+180,1000);
      if (tp_number<IGCview.task.tp_count)
        IGCview.drawLineP(g,x,y,IGCview.task.track[tp_number+1],1000);
      IGCview.task.draw_sector(g, this, tp_number);
      if (IGCview.primary_index!=0)
        {
          TrackLog log = IGCview.logs[IGCview.primary_index];
          log.draw(this, g, IGCview.config.PRIMARYCOLOUR);
          if (log.tps_rounded>=tp_number)
            log.mark_point(this,g,IGCview.config.SECTORCOLOUR.colour,log.tp_index[tp_number]);
        }
  }

  public int long_to_x(float longitude)
    {
      return (int) ((longitude - zoom.longitude) * zoom.xscale);
    }

  public int lat_to_y(float latitude)
    {
      return (int) ((zoom.latitude - latitude) * zoom.yscale);
    }

  float x_to_long(int x)
    {
      return (float) x / zoom.xscale + zoom.longitude;
    } 

  float y_to_lat(int y)
    {
      return zoom.latitude - ((float) y / zoom.yscale);
    }

  public void mousePressed (MouseEvent e)
    {
      frame.current_canvas = tp_number;
      if (frame.ruler)
        {
          Graphics g = getGraphics();
          g.setColor(IGCview.config.MAPBACKGROUND.colour);
          g.setXORMode(Color.white);
          g.drawLine(rect_x, rect_y, prev_rect_x1, prev_rect_y1);  // remove old line
        }
      rect_x = e.getX();
      rect_y = e.getY();
      prev_rect_x1 = rect_x;
      prev_rect_y1 = rect_y;
      if (frame.ruler) update_ruler();
    }

  public void mouseReleased (MouseEvent e)
    { 
      if (frame.ruler) return;
      if (rect_x1-rect_x<5 || rect_y1-rect_y<5)
        {
          return;
        }
      float x_adj = ((float) width / (float) (rect_x1-rect_x));
      float y_adj = ((float) height / (float) (rect_y1-rect_y));
      if (x_adj > y_adj) x_adj = y_adj;
      zoom.scale_corner(y_to_lat(rect_y), x_to_long(rect_x), x_adj);
      prev_rect_x1 = 0;
      prev_rect_y1 = 0;
      rect_x1 = 0;
      rect_y1 = 0;
      paint(getGraphics());
    }

  public void mouseDragged (MouseEvent e)
    {
      rect_x1 = e.getX();
      rect_y1 = e.getY();
      if (rect_x==0 && rect_y==0) // fix for mousePressed just got focus 
        {
          rect_x = rect_x1;
          rect_y = rect_y1;
        }
      if (frame.ruler) update_ruler();
      repaint();
    }
 
  public void mouseClicked(MouseEvent e) {};
  public void mouseEntered(MouseEvent e) {};
  public void mouseExited(MouseEvent e) {};
  public void mouseMoved(MouseEvent e) {};

  void update_ruler()
    {
      float lat1=y_to_lat(rect_y),
            long1=x_to_long(rect_x),
            lat2=y_to_lat(rect_y1),
            long2=x_to_long(rect_x1);
      RulerWindow.latitude = lat2;
      RulerWindow.longitude = long2;
      RulerWindow.dist = IGCview.dec_to_dist(lat1,long1,lat2,long2);
      RulerWindow.track = IGCview.dec_to_track(lat1,long1,lat2,long2);
      RulerWindow.update();
    }
}

//***************************************************************************//
//            ScoringFrame                                                   //
//***************************************************************************//


class ScoringFrame extends Frame implements ActionListener,
                                            WindowListener
{
  String units;

  private static String [] [] menus = {
    {"File", "Close"}
  };    

  int width, height;
  TextArea text = new TextArea();

  ScoringFrame ()
    {
      setTitle("Scoring Data");
      width = IGCview.config.SCORINGWIDTH;
      height = IGCview.config.SCORINGHEIGHT;
      setSize(width, height);
      setFont(new Font("Helvetica", Font.PLAIN, 12));
      this.addWindowListener(this);
      initialise();
    }

  void initialise()
    {
      text.setEditable(false);
      add(text,"Center");
	System.out.println("ScoringFrame: added text");
      MenuBar menubar = new MenuBar ();
      this.setMenuBar (menubar);

      for (int c = 0; c < menus.length; c++) {
        Menu m = new Menu (menus [c][0]);
        menubar.add (m);
        for (int r = 1; r < menus [c] .length; r++) {
  	  if (menus [c][r] == null)
              m.addSeparator ();
	  else
            {
	      MenuItem i = new MenuItem (menus [c][r]);
	      m.add (i);
	      i.setActionCommand (menus [c][r] .toLowerCase ());
	      i.addActionListener (this);
  	    }
        }
      }
      refresh();
      if (IGCview.scoring_window!=null) this.pack ();
    }

  public Dimension getPreferredSize ()
    {
      return new Dimension (width, height);
    }

  void write(String s)
    {
      text.append(s);
    }

  public void actionPerformed(ActionEvent e)
    {
      String label = e.getActionCommand();
      if (label.equals ("close"))
        { dispose();
	    IGCview.scoring_window = null;
          return;
	  }
    }

  void refresh()
    {
      print_task_data();
      print_log_headings();
      for (int log_num=1; log_num<=IGCview.log_count; log_num++)
        if (log_num==IGCview.primary_index || IGCview.secondary[log_num])
          print_log_data(log_num);
    }

  void print_task_data()
    {
      write("\nTask:\t");
      for (int i=1;i<IGCview.task.tp_count; i++) write(IGCview.task.tp[i].trigraph+" - ");
      write(IGCview.task.tp[IGCview.task.tp_count].trigraph + "\n\n");
      units = IGCview.config.convert_task_dist==IGCview.NMKM ? " km" :
	         (IGCview.config.convert_task_dist==(float)1.0 ? " nm" : " miles");
      write("Task length: "+
            IGCview.places(IGCview.task.length*IGCview.config.convert_task_dist,2) +units+".\n\n");
    }

  void print_log_headings()
    {
      write("Glider\tStart time\tFinish time\tSpeed ("+units+"/hour)\tDistance ("+units+" )\tLat\t\tLong\n");
    }

  void print_log_data(int log_num)
    {
      TrackLog log = IGCview.logs[log_num];
      log.calc_flight_data();
      write(log.name+"\t");
      if (log.tps_rounded>0)
        {
          write(IGCview.format_clock(log.time[log.tp_index[1]])+"\t");
          if (log.tps_rounded==IGCview.task.tp_count)        
            {
              write(IGCview.format_clock(log.time[log.tp_index[log.tps_rounded]])+"\t\t");
              write(IGCview.places(log.task_speed*IGCview.config.convert_task_dist,2));
            }
          else
            {
              write("LAND OUT\t\t\t");
              write(IGCview.places(log.task_distance*IGCview.config.convert_task_dist,2)+units+"\t");
              write(IGCview.format_lat(log.latitude[log.landed_index])+"\t");
              write(IGCview.format_long(log.longitude[log.landed_index]));
            }
        }
      else if (log.gps_ok)
        write("NO VALID START.");
      else
        write("No GPS data in log.");
      write("\n\n");
    }

  // WindowListener interface:
  public void windowActivated(WindowEvent e) {;}
  public void windowClosed(WindowEvent e) {;}
  public void windowClosing(WindowEvent e)
    {
      dispose();
	IGCview.scoring_window = null;
    }

  public void windowDeactivated(WindowEvent e) {;}
  public void windowDeiconified(WindowEvent e) {;}
  public void windowIconified(WindowEvent e) {;}
  public void windowOpened(WindowEvent e) {;}
} // end of ScoringFrame

//***************************************************************************//
//            Map_db                                                         //
//***************************************************************************//

// class Map_db handles the storage and display of background (map) images on the canvas
class Map_db
{ 

  Map [] map = new Map [IGCview.MAXIMAGES+1];  // Object array for map GIF/JPEG's and coords
                                                // first map is map[1], last is map[map_count]
  int map_count = 0;

  MediaTracker mt;  // This object will allow you to control loading 

  Map_db() {;} // default constructor
  
  void load(String map_list_name)
    {
     String buf;
     try {
          URL list_url;
          if (map_list_name.startsWith("http")) list_url=new URL(map_list_name);
          else list_url= new URL(IGCview.ApIGCview.getDocumentBase(), map_list_name);

          mt = new MediaTracker(IGCview.ApIGCview);   // initialize the MediaTracker 

          BufferedReader in = new BufferedReader(new InputStreamReader(list_url.openStream()));
          while ((buf = in.readLine()) != null) 
            {
              if (buf.length()<10 || buf.charAt(0)==';') continue; // ';' signifies a comment
              Map new_map = new Map();
              if (new_map.setup(buf)) // create new map but don't load yet
                {
                  map[++map_count] = new_map;
                }
            }
          in.close();
          }
     catch (FileNotFoundException e) { System.err.println("ImageDisplayer.load: " + e);} 
     catch (MalformedURLException e) { System.err.println("ImageDisplayer.load: " + e);}
     catch (Exception e) { System.err.println("ImageDisplayer.load: " + e);}
    } 

  void load_images() // called by jav_load_images() in the java thread managed by Dispatcher
    {                // The getImage library routine is also asynchronous, using MediaTracker
      URL base = null;  // The applet base URL
      try
        { 
          base = IGCview.ApIGCview.getDocumentBase();   // getDocumentbase gets the applet path. 
        } 
      catch (Exception e) {  IGCview.msgbox("IGCview: load base exception: "+e);} 
      for (int i = 1; i<=map_count; i++)
        {
          if (map[i].status!=Map.DO_LOAD) continue; // only process images flagged to be loaded
          try
            {
              map[i].image = IGCview.ApIGCview.getImage(base,map[i].name);   // Here we initiate the GIF file read.
            }
          catch (Exception e)
            { IGCview.msgbox("IGCview: error loading map image "+map[i].name);
              map[i].status = Map.ERROR;
              return;
            } 
          map[i].status = Map.LOADING;  
          mt.addImage(map[i].image,1);   // tell the MediaTracker to keep an eye on this image, and give it ID 1;
        }
    }
    
  void draw(Graphics g, DrawingCanvas d)  
    {
      if (map_count==0)
        {
                   // put mapfile load into asynchronous java thread for security
          IGCview.ApIGCview.igc_load_mapfile(IGCview.config.MAPFILE);
        }
      IGCview.show_status("");
      // flag loads of READY ONSCREEN images
      boolean load_flagged = false; // boolean set if any draw_image() flags a load
      for (int i=1; i<=map_count; i++)
        {
          if (map[i].status==Map.READY) load_flagged = ( draw_image(g,d,i) ? true : load_flagged );
        } // end for

      // if any loads were flagged then call igc_load_images
      if (load_flagged)
        IGCview.ApIGCview.igc_load_images(); // use Dispatcher to schedule jav_load_images()
                                             // this shifts the image loads into an asynchronous java thread
                                             // rather than the current javascript thread
        
      // draw LOADED images
      for (int i=1; i<=map_count; i++)
        {
          if (map[i].status==Map.ERROR) continue; // skip bad maps
          if (map[i].status==Map.DO_LOAD) continue; // skip maps already flagged for loading
          draw_image(g,d,i);              
        } // end for
      // wait for new image loads
      IGCview.show_status("Loading map images...");
      wait_images();
      IGCview.show_status("");
      // draw new LOADED images
      for (int i=1; i<=map_count; i++)
        {
          if (map[i].status==Map.LOADING)
            {
              map[i].status = Map.LOADED; // assume LOADING now LOADED as previous wait_images()
              draw_image(g,d,i);
            }              
        } // end for

    }  // end draw()

  // draw map image i if onscreen, or flag it for loading
  boolean draw_image(Graphics g, DrawingCanvas d, int i) // returns true if any image flagged for loading
    {
      // now we are going to draw the gif on the screen 
      int x1,y1,x2,y2; // x,y coords of scaled full image
      int cx,cy,cw,ch; // coords of sub-image to crop out of full image for display
      int x,y,w,h;     // coords for display on canvas
      
      int canvas_w = d.width, canvas_h = d.height; // width and height of canvas
      int image_w, image_h; // width and height of full image
      Image cropped_image; // holder for cropped image for display on canvas
      ImageFilter f;

      // calculate (x1,y1)/(x2,y2) screen coords of corners of full image
      x1 = d.long_to_x(map[i].long_min); // x1,y1 = top left corner of image
      if (x1>canvas_w) return false; // skip this image if outside canvas
      y1 = d.lat_to_y(map[i].lat_max);  
      if (y1>canvas_h) return false; // skip this image if outside canvas
      x2 = d.long_to_x(map[i].long_max); // x2,y2 = bottom right of full image
      if (x2<0) return false; // skip this image if outside canvas
      y2 = d.lat_to_y(map[i].lat_min);
      if (y2<0) return false; // skip this image if outside canvas

      // here image must appear on canvas
      if (map[i].status==Map.READY) // if 'READY' then load it
        {
          map[i].status = Map.DO_LOAD; // loading initiated
          return true; // skip to next image
        }
          
      // status must be 'LOADED'
          
      // calculate coords within full image to crop
      image_w = map[i].image.getWidth(IGCview.ApIGCview);
      image_h = map[i].image.getHeight(IGCview.ApIGCview);
          
      if (x1<0 && x2>canvas_w && y1<0 && y2>canvas_h) // image includes canvas
        {
          // cx,cy,cw,ch are coords for crop within image
          cx = (int) ((float)(-x1)/(x2-x1)*image_w);
          cy = (int) ((float)(-y1)/(y2-y1)*image_h);
          cw = (int) ((float) canvas_w/(x2-x1)*image_w);
          ch = (int) ((float) canvas_h/(y2-y1)*image_h);
            
          // produce new image as cropped subset of full image
          f = new CropImageFilter(cx,cy,cw,ch);
          cropped_image = IGCview.ApIGCview.createImage(new FilteredImageSource(map[i].image.getSource(), f));
          mt.addImage(cropped_image,1);   // tell the MediaTracker to keep an eye on this image, and give it ID 1;
          try // now wait for image crop to complete
            { 
              mt.waitForAll(); 
            } 
          catch (InterruptedException  e) {} 
          g.drawImage(cropped_image, 0, 0, canvas_w, canvas_h, IGCview.ApIGCview);
        }
      else if (x1>0 && x2<canvas_w && y1>0 && y2<canvas_h) // image included by canvas
        {
          // x,y,w,h are coords for image to fit on canvas
          x = x1;
          y = y1;
          w = x2 - x1;
          h = y2 - y1;
          // if (w<20 || h<20) continue; // don't draw if smaller than 20 x 20 pixels
          g.drawImage(map[i].image, x, y, w, h, IGCview.ApIGCview);
        }
      else // image overlaps canvas
        {
          // cx,cy,cw,ch are coords for crop within image
          // x,y,w,h are coords for image to fit on canvas
          // calc cx, x
          if (x1>0) { cx = 0; x = x1; }
          else { cx = (int) ((float)(-x1)/(x2-x1)*image_w); x = 0; }
          
          // calc cy, y
          if (y1>0) { cy = 0; y = y1; }
          else { cy = (int) ((float)(-y1)/(y2-y1)*image_h); y = 0; }
          
          // calc cw
          if (x2<canvas_w)
            if (x1>0) { cw = image_w; }
            else { cw = (int) ((float) x2/(x2-x1)*image_w); }
          else          
            if (x1>0) { cw = (int) ((float) (canvas_w-x1)/(x2-x1)*image_w); }
            else { cw = (int) ((float) canvas_w/(x2-x1)*image_w); }
          // calc ch
          if (y2<canvas_h)
            if (y1>0) { ch = image_h; }
            else { ch = (int) ((float) y2/(y2-y1)*image_h); }
          else          
            if (y1>0) { ch = (int) ((float) (canvas_h-y1)/(y2-y1)*image_h); }
            else { ch = (int) ((float) canvas_h/(y2-y1)*image_h); }

          // calc w, h
          w = (x2<canvas_w) ? x2-x : canvas_w-x;
          h = (y2<canvas_h) ? y2-y : canvas_h-y;
         
          f = new CropImageFilter(cx,cy,cw,ch);
          cropped_image = IGCview.ApIGCview.createImage(new FilteredImageSource(map[i].image.getSource(), f));
          mt.addImage(cropped_image,1);   // tell the MediaTracker to keep an eye on this image, and give it ID 1;
          wait_images();
          g.drawImage(cropped_image, x, y, w, h, IGCview.ApIGCview);
        }
      return false;
    } // end draw_image()

  void wait_images()
    {
      try // wait for any remaining image loads
        { 
          mt.waitForAll(); 
        } 
      catch (InterruptedException  e) {}
    } // end wait_images()
    
} // end of Map_db

//***************************************************************************//
//            Map                                                            //
//***************************************************************************//

// class ImageDisplayer handles the display of background (map) images on the canvas
class Map 
{
  // some status constants

  static final int READY = 1; // valid entry in map image list file, ready for loading when required  
  static final int DO_LOAD = 2;  // flag to tell Map.load_images() to load this GIF
  static final int LOADING = 3;  // flag set by Map.load_images() when file load has been initiated
  static final int LOADED = 4;  // flag set in Map.draw() when mediatracker has confirmed image loaded
  static final int ERROR = 5;
  
  // Map class variables
    
  Image image;
  float lat_min = (float) 0.0; 
  float lat_max = (float) 0.0; 
  float long_min = (float) 0.0; 
  float long_max = (float) 0.0;
  float min_scale = (float) 200.0; // current zoom scale must be above this value for image to be displayed
  String name; // file name of GIF
  int status = ERROR; 

  Map() {;}
  
  boolean setup(String buf) // will return false if error parsing parameters  
    {
      // map_info format is: filename,lat_max,long_min,lat_min,long_max
      // i.e. the lat/longs are top left corner, bottom right corner.
      // the format for a lat/long is e.g. "49 76.123N".  Any number of decimals ok, N can be at start or end
      
      int c1, c2, c3, c4; // c1..c4 is index of each comma
       
      // try and confirm a valid record before processing
      if (buf.length()<9) return false;
      c1 = buf.indexOf(",");
      if (c1<=0) return false;
      c2 = buf.indexOf(",", c1+1);
      if (c2<=0) return false;
      c3 = buf.indexOf(",", c2+1);
      if (c3<=0) return false;
      c4 = buf.indexOf(",", c3+1);
      if (c4<=0) return false;
      name = buf.substring(0,c1);
      lat_max = IGCview.parse_latlong(buf.substring(c1+1,c2));
      long_min = IGCview.parse_latlong(buf.substring(c2+1,c3));
      lat_min = IGCview.parse_latlong(buf.substring(c3+1,c4));
      long_max = IGCview.parse_latlong(buf.substring(c4+1));
      if (lat_max==(float) 0.0 || long_min==(float) 0.0 || lat_min==(float) 0.0 || long_max==(float) 0.0)
        return false;
      status = READY; 
      return true;
    }
    
} // end of class Map
