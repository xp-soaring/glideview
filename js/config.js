function Config()
{

  var self = this;

  // WINDOWS
  self.SELECTWIDTH = 400; // window default sizes
  self.SELECTHEIGHT = 300;
  self.SCORINGWIDTH = 780;
  self.SCORINGHEIGHT = 500;
  self.CONFIGWIDTH = 600;
  self.CONFIGHEIGHT = 500;
  self.TPWIDTH = 700;
  self.TPHEIGHT = 600;
  self.TPROWS = 3; // number of rows/columns in TP detail window
  self.TPCOLS = 2;

  // MAP CONFIG
  self.draw_pri_thermals = true;
  self.draw_pri_thermal_ends = false;
  self.draw_pri_thermal_strengths = true;
  self.draw_sec_thermals = true;

  self.draw_tp_times = true;
  self.draw_tps = true;
  self.draw_tp_names = true;
  self.trim_logs = true;
  self.upside_down_lollipops = false;
  self.glider_icons = true;
  self.draw_sectors = true;

  self.draw_images = false; // load and draw map GIF's

  self.beer_can_sectors = true;    // beercan sector (i.e. cylinder)
  self.photo_sectors = true;    // 90 degree photo sector
  self.beer_tp_radius = 0.2699784; // nm = 500m
  self.photo_tp_radius = 1.6198704; // nm = 3km
  self.startline_180 = true;        // perpendicular start line (vs. photo sector)
  self.startline_radius = 3.2397408; // nm = 3km
  self.landout_max_distance = true; // true => landout point is at max flight distance,
                                       // rather than actual landed point

  self.map_position = { latitude: 52.5, longitude: -0.5, scale: 10 };

  self.auto_zoom = true; // automatically calls 'zoom_to_flight' when new primary loaded

  self.task_draw_options = { color: 'red', weight: 3, opacity: 0.5 };

  self.tracklog_draw_options = { color: 'blue', weight: 1, opacity: 1.0 };

  /*
  // COLOURS
  self.MAPBACKGROUND = new ConfigColour(8,2); // ConfigColour(ColourIndex, ColourShade)
  self.CURSORCOLOUR = new ConfigColour(1,1);  // where ColourIndex is listed below
  self.SECTRACKCOLOUR = new ConfigColour(8,1);// and ColourShade: 0=darker, 1=lighter, 2=normal
  self.SECAVGCOLOUR = new ConfigColour(8,2);
  self.ALTCOLOUR = new ConfigColour(0,1);          // primary trace in ALT window
  self.ALTSECCOLOUR = new ConfigColour(8,2);       // secondary traces in ALT window
  self.TPBARCOLOUR = new ConfigColour(0,1);        // vertical lines in ALT windows
  self.TASKCOLOUR = new ConfigColour(1,1);         // task drawn on map
  self.TPTEXT = new ConfigColour(5,0);             // text colour of TP's on map
  self.TPCOLOUR = new ConfigColour(5,0);           // colour of TP's on map
  self.CLIMBCOLOUR = new ConfigColour(0,1);        // climb bars in CLIMB window
  self.CLIMBSECCOLOUR = new ConfigColour(8,2);     // secondary climb bars
  self.CRUISECOLOUR = new ConfigColour(0,1);       // speed bars in CRUISE window
  self.CRUISESECCOLOUR = new ConfigColour(8,2);    // secondary cruise bars
  self.SECTORCOLOUR = new ConfigColour(1,0);       // sector lines on tp detail window

  self.MAXCOLOURS = 10;
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
*/
  // tp database handling
  self.startup_load_tps = true;	// load tp database file at startup

  // FILES
     //static String TPFILE = "G:\\src\\java\\igcview\\bga.gdn"; // TP gardown file
     //static String LOGDIR = "G:\\igcfiles\\grl97\\grl1\\";     // LOG directory
  //String TPFILE = "turnpoints/bga.gdn"; // turnpoint file
  //String LOGDIR = "."+System.getProperty("file.separator");
  //String MAPFILE = "map_data/map_data.txt"; // image list text file

  // MAGGOT RACE PARAMETERS
  self.RACEPAUSE = 100; // pause 100ms between screen updates
  self.altsticks = true; // draw altitude stick below maggots

  // UNIT CONVERSIONS             // Stored As:   Start Default: Conversion permitted:
  self.convert_task_dist = 1.852; // NM           KM             1.0 NMKM(km) NMMI(mi)
  self.convert_speed = 1.0;       // KT           KT             1.0 NMKM(kmh) NMMI(mph)
  self.convert_altitude = 1.0;    // FT           FT             1.0 FTM(m)
  self.convert_climb = 1.0;       // KT           KT             1.0 KTMS(m/s)
  self.time_offset = 0;                     // time zone adjustment

  return self;
} // Config
