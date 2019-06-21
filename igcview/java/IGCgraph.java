import java.applet.*;
import java.awt.*;
import java.awt.List;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.math.*;
import java.lang.*;

public class IGCgraph extends Applet
{
  // graph window constants (i.e. altitude, climb, cruise windows)

  static int   BASELINE = 15;         // offset of zero axis from bottom of graph frame
                                           // maybe should be higher if legend to be included

  static int   MINALT = 0;
  static int   MAXALT = 18000;        // altitude window y-axis = 0..MAXALT feet
  static final int   ALTMAXDIST = 750;     // altitude window x-axis = 0..MAXALTDIST nm
  static int   GRAPHTIME = 86400;    // altitude graph x-axis max is 24 hours  

  static final int   MAXCLIMB = 10;        // climb window 0..MAXCLIMB
  static final int   MAXCRUISE = 120;      // cruise window 0..MAXCRUISE

  static final int   CLIMBAVG = 30;        // averaging period for thermalling()
  static final int   CLIMBSPEED = 40;      // point-to-point speed => thermalling

  static final float TIMEXSCALE = (float) 0.04;  // scale for time x-axis in ALT windows
  static final float DISTXSCALE = (float) 4;     // scale: distance x-axis in ALT windows
  static final float DISTSTART = (float) 25;     // start offset for distance X-axis (nm)

  static final int TIMEX=1, DISTX=2; // constants giving type of x-axis
  static final int GRAPHALT=1, GRAPHCLIMB=2, GRAPHCRUISE=3, GRAPHCP=4;

  static final int   CLIMBBOXES = 30;      // number of categories of climb
  static final float CLIMBBOX = (float) 0.5;  // size of climb category in knots
  static final int   MAXPERCENT = 50;     // max %time in climb profile window
  static final int   LASTTHERMALLIMIT = 120; // ignore 'thermal' within 120secs of end

  public IGCview ApIGCview = null;

  ApGraphCanvas canvas;
  int width, height;
  int graph_type = GRAPHALT;
  TrackLog log;

  public void init()
    { 
      int i;
      int TIMEOUT = 120;
      for (i=0; i<TIMEOUT; i++)
        {
    	    try { java.lang.Thread.sleep(500); }
          catch (InterruptedException e){ ; }

          ApIGCview = (IGCview) getAppletContext().getApplet("IGCview");
          if (ApIGCview!=null) break;	// exit loop if we've found the IGCview applet
        }
      if (ApIGCview == null) IGCview.msgbox("Oops, IGCview applet not loaded successfully");
      // else IGCview.msgbox("IGCgraph.init(): IGCview applet found after "+i+" seconds");


      try { width = Integer.valueOf(getParameter("WIDTH")).intValue(); }
      catch (NullPointerException e) { width = 100; }

      try { height = Integer.valueOf(getParameter("HEIGHT")).intValue(); }
      catch (NullPointerException e) { height = 100; }

      boolean in_sx=false;
      String x;
      if(null!=(x=getParameter("TIME_SPAN")) && x.length()>0)
        {
          in_sx=true;
          GRAPHTIME=Integer.valueOf(x).intValue();
        }
      if(null!=(x=getParameter("MAX_ALTITUDE")) && x.length()>0)
        MAXALT=(int)(Integer.valueOf(x).intValue()/IGCview.FTM);
      if(null!=(x=getParameter("MIN_ALTITUDE")) && x.length()>0)
        MINALT=(int)(Integer.valueOf(x).intValue()/IGCview.FTM);

      setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

      if(in_sx)
        {
          canvas = new ApGraphCanvas(this);
          this.add(canvas);
          canvas.set4SX();
        }
      else
        {
          ScrollPane pane = new ScrollPane (ScrollPane.SCROLLBARS_AS_NEEDED);
          pane.setSize(width,height);
          canvas = new ApGraphCanvas(this);
          pane.add(canvas);
          pane.setScrollPosition(0,height-50);
          //repaint();
          pane.doLayout();
          this.add (pane);
        }

      doLayout();
    } 

  public Dimension getPreferredSize ()
    {
      return new Dimension (width, height);
    }

  // User Interface Routines

  public void igc_setalt()
    {
      if (ApIGCview == null) ApIGCview = (IGCview) getAppletContext().getApplet("igcview");
      if (ApIGCview == null) IGCview.msgbox("Oops, IGCview applet not found successfully");

      canvas.setalt();
    }

  public void igc_setclimb()
    {
      canvas.setclimb();
    }

  public void igc_setcruise()
    {
      canvas.setcruise();
    }

  public void igc_setcp()
    {
      canvas.setcp();
    }

  public void igc_settime()
    {
      canvas.set_time_x_axis();
    }

  public void igc_setdist()
    {
      canvas.set_dist_x_axis();
    }

  public void igc_expand()
    {
    	canvas.expand();
    }

  public void igc_compress()
    {
	    canvas.compress();
    }

  public void igc_expand_alt()
    {
    	canvas.expand_alt();
    }

  public void igc_compress_alt()
    {
	    canvas.compress_alt();
    }

}

//***************************************************************************//
//            ApGraphCanvas                                                  //
//***************************************************************************//

class ApGraphCanvas extends Panel implements MouseListener,
                                            MouseMotionListener,
                                            RacingCanvas,
                                            KeyListener,
                                            Graphable
{
  public int width, height;
  public float  min_y=0, max_y, major_incr_y = 5000, minor_incr_y = 1000;

  int init_width, init_height;
  int time_start;
  IGCgraph frame;
  public int x_axis = IGCgraph.TIMEX;
  float xscale = IGCgraph.TIMEXSCALE,
	  yscale;
  int cursor1_x=1, cursor2_x=1;
  boolean racing = false;
  int graph_type = IGCgraph.GRAPHALT;

  // ApGraphCanvas(){;}

  ApGraphCanvas(IGCgraph frame)
    {
      this.frame = frame;
      setalt();
      this.addMouseListener (this);
      this.addMouseMotionListener (this);
      this.addKeyListener(this);
    }

  public int x_axis()
    {
      return x_axis;
    }

  void set4SX()
    {
      init_width=frame.width;
      width=init_width;
      init_height=frame.height;
      height=init_height;
      setSize(width,height);
  
      //System.out.println("width: "+width+"; GRAPHTIME: "
      //		     +IGCgraph.GRAPHTIME+"; xscale: "+xscale);
      xscale=(float)width/(float)IGCgraph.GRAPHTIME;

      min_y=IGCgraph.MINALT;
      max_y=IGCgraph.MAXALT;
      yscale=(float)height/(float)(max_y-min_y);

      //System.out.println("alt: "+min_y+"-"+max_y+"; scale: "+yscale);
      //System.out.println("height: "+height);

      IGCgraph.BASELINE=0;

      repaint();
    }

  void setalt()
    {
      //System.out.println("ApGraphCanvas.setalt: size1 = "+getSize().width+","+getSize().height);
      //System.out.println("ApGraphCanvas.setalt: w,h = "+width+","+height);
      init_width = 6*frame.width;
      init_height = frame.height-10;
      if (graph_type==IGCgraph.GRAPHCP)
        {
          xscale = IGCgraph.TIMEXSCALE;
          x_axis = IGCgraph.TIMEX;
        }
      graph_type = IGCgraph.GRAPHALT;
      min_y=IGCgraph.MINALT;
      max_y = IGCgraph.MAXALT;
      width = init_width;
      height = init_height;
      yscale = (float) (height-IGCgraph.BASELINE) / (max_y-min_y);
      //debug:
      //if (frame.ApIGCview==null)
      //  {
      //    IGCview.msgbox("setalt(): frame.ApIGCview==null");
      //    return;
      //  }
      if ((frame.ApIGCview==null) || (frame.ApIGCview.config.convert_altitude==(float)1.0))
        {
          major_incr_y = 5000;
          minor_incr_y = 1000; // feet
        }
      else
        {
          major_incr_y = (float) 3280.84;
          minor_incr_y = (float) 1640.42; // meters
        }
      this.setSize(width, height);
      set_time_start();
      this.repaint();
    }

  void setclimb()
    {
      if (graph_type==IGCgraph.GRAPHCP)
        {
          xscale = IGCgraph.TIMEXSCALE;
          x_axis = IGCgraph.TIMEX;
        }
      graph_type = IGCgraph.GRAPHCLIMB;
      init_width = 6*frame.width;
      init_height = frame.height-30;
	max_y = IGCgraph.MAXCLIMB;
      yscale = (float) (height-IGCgraph.BASELINE) / (max_y-min_y);
      width = init_width;
	height = init_height;
	//debug:
	if (frame.ApIGCview==null)
	  {
	    IGCview.msgbox("setclimb(): frame.ApIGCview==null");
	    return;
 	  }
      if (frame.ApIGCview.config.convert_climb==(float)1.0)
	  {
	    major_incr_y = 5;
	    minor_incr_y = 1; // knots
        }
      else
	  {
	    major_incr_y = (float) 1.944;
	    minor_incr_y = (float) 0.972; // m/s
        }
	this.repaint();
    }

  void setcruise()
    {
      if (graph_type==IGCgraph.GRAPHCP)
        {
          xscale = IGCgraph.TIMEXSCALE;
          x_axis = IGCgraph.TIMEX;
        }
      graph_type = IGCgraph.GRAPHCRUISE;
      init_width = 6*frame.width;
      init_height = frame.height-30;
	max_y = IGCgraph.MAXCRUISE;
      yscale = (float) (height-IGCgraph.BASELINE) / (max_y-min_y);
      width = init_width;
	height = init_height;
	//debug:
	if (frame.ApIGCview==null)
	  {
	    IGCview.msgbox("setalt(): frame.ApIGCview==null");
	    return;
 	  }
      major_incr_y = (float) 50/frame.ApIGCview.config.convert_speed;
      minor_incr_y = (float) 10/frame.ApIGCview.config.convert_speed;
	this.repaint();
    }

  void setcp()
    {
      graph_type = IGCgraph.GRAPHCP;
      init_width = 2*frame.width;
      init_height = frame.height-30;
      width = init_width;
      height = init_height;
      xscale = 2* ((float) (width - IGCgraph.BASELINE)) / IGCgraph.CLIMBBOXES;
                                                         // xscale in pixels/box
      yscale = ((float) (height - IGCgraph.BASELINE)) / IGCgraph.MAXPERCENT;
                                                         // yscale in pixels/(%time)
      this.repaint();
    }

  public Dimension getPreferredSize () 
    {
      //System.out.println("ApGraphCanvas.getPreferredSize(): "+width+","+height);
      return new Dimension (width, height);
    }

  public void paint (Graphics g)
    { Dimension d = this.getSize();
      //System.out.println("ApGraphCanvas.paint: "+d);
      g.setColor(Color.white);
      g.fillRect(0,0, d.width - 1, d.height - 1);
      draw_grid(g);                                        // draw grid
      if ((frame.ApIGCview==null) || (frame.ApIGCview.primary_index == 0)) return;
      switch (graph_type)
        {
          case IGCgraph.GRAPHALT:
            if (racing)
              {
                draw_legend(g);
                frame.ApIGCview.logs[frame.ApIGCview.primary_index].draw_alt_tp_lines(this,g);
              }
            else
              {
                frame.ApIGCview.sec_log.draw_alt(this,g);  // draw secondaries
                frame.ApIGCview.logs[frame.ApIGCview.primary_index].draw_alt(this, g, true); // draw primary
              }
            break;
          case IGCgraph.GRAPHCLIMB:
            frame.ApIGCview.sec_log.draw_climb(this, g);
            frame.ApIGCview.logs[frame.ApIGCview.primary_index].draw_climb(this, g, true);
            break;
          case IGCgraph.GRAPHCRUISE :
            frame.ApIGCview.sec_log.draw_cruise(this,g);           // draw secondaries
            frame.ApIGCview.logs[frame.ApIGCview.primary_index].draw_cruise(this, g, true);   // draw primary
            break;
          case IGCgraph.GRAPHCP :
            frame.ApIGCview.sec_log.draw_climb_profile(this,g);       // draw secondary
            frame.ApIGCview.logs[frame.ApIGCview.primary_index].draw_climb_profile(this, g, true);      // draw primary
            break;
        }      
    }

  void set_time_start()
    {
      int i=1;
	if (frame.ApIGCview == null)
	  {
	    // m = new MsgBox("set_time_start(): frame.ApIGCview == null");
	    return;
	  }
	if (frame.ApIGCview.primary_index == 0) return; // return if no logs loaded
	//MsgBox m11 = new MsgBox("In set_time_start() - 11");
	TrackLog log = frame.ApIGCview.logs[frame.ApIGCview.primary_index];
/*
      if (log.tps_rounded>0)   // graph from 1 hour before start, round down to nearest hour
        time_start = (log.time[log.tp_index[1]] / 3600 - 1) * 3600; 
      else
        {                           // skip to 3 mins after start of log
          while (i<log.record_count &&
                 log.time[i]-log.time[1]<180) i++;
          time_start = (log.time[i] / 3600) * 3600;
        }
*/
          while (i<log.record_count &&
                 log.get_speed(i)<5) i++;
	  time_start=log.time[i]; // takeoff
	  //time_start-=10*60;
    }
  
  void set_time_x_axis()
        {
          x_axis = IGCgraph.TIMEX;
          xscale = IGCgraph.TIMEXSCALE;
          width = init_width;
          height = init_height;
          //setSize(new Dimension(width, height));
          frame.doLayout();
	    this.repaint();
        }

  void set_dist_x_axis()
        {
          if (frame.ApIGCview.task==null || !frame.ApIGCview.logs[frame.ApIGCview.primary_index].gps_ok)
            {
              IGCview.msgbox("Cannot view by distance if no task defined or no GPS data in log");
              return;
            }
          x_axis = IGCgraph.DISTX;
          xscale = IGCgraph.DISTXSCALE;
          width = init_width;
          height = init_height;
          setSize(new Dimension(width, height));
          frame.doLayout();
          this.repaint();
        }

  void expand()
    {
      xscale *= 2;
      width=2*width;
      setSize(new Dimension(width, height));
      frame.doLayout();
    }

  void compress()
    {
      xscale /= 2;
      width=width/2;
      setSize(new Dimension(width, height));
      frame.doLayout();
    }

  void expand_alt()
    {
      yscale *= 2;
      // height = 2*height;
      // setSize(new Dimension(width, height));
      // frame.doLayout();
      paint(getGraphics());
    }

  void compress_alt()
    {
      yscale /= 2;
      // height = height/2;
      // setSize(new Dimension(width, height));
      // frame.doLayout();
      paint(getGraphics());
    }

  void draw_grid(Graphics g)
    {
      int line_x, line_y;

      if (graph_type==IGCgraph.GRAPHCP) { draw_cp_grid(g); return; }

      g.setColor(Color.gray);
      for (float i=0; i<max_y; i+=major_incr_y)  // draw horizontal lines
        {
          line_y = get_y(i);
          g.drawLine(0, line_y, width, line_y);
          g.drawLine(0, line_y+1, width, line_y+1); // double lines for major increments
	    for (float j=minor_incr_y; j<major_incr_y; j+=minor_incr_y)
            {
              line_y = get_y(i+j);
              g.drawLine(0, line_y, width, line_y);
            }
        }
      if (x_axis()==IGCgraph.TIMEX)    // draw vertical lines
        draw_time_contours(g);
      else
        draw_dist_contours(g);
      g.setColor(Color.black);
      line_y = get_y((float) 0);           // draw baseline
      g.drawLine(0, line_y, width, line_y);
      g.drawLine(0, line_y+1, width, line_y+1);
    }

  void draw_cp_grid(Graphics g)
    {
      int line_x, line_y;

      g.setColor(Color.gray);
      for (int i=10; i<=IGCgraph.MAXPERCENT; i+=10) // draw %time contours
        {
          line_y = get_y((float) i);
          g.drawLine(0, line_y, width, line_y);
        }
                                  // draw vertical grid lines
      for (float climb=0;
           climb<=IGCgraph.CLIMBBOX*IGCgraph.CLIMBBOXES;
           climb+=(float) 1 / frame.ApIGCview.config.convert_climb)
        {
          line_x = climb_to_x(climb);
          g.drawLine(line_x, 0, line_x, height);
          g.drawLine(line_x+1, 0, line_x+1, height); // double line for full climb units
          if (frame.ApIGCview.config.convert_climb!=(float)1.0)
	    {
              line_x = climb_to_x(climb+(float)1/(frame.ApIGCview.config.convert_climb*2));
              g.drawLine(line_x, 0, line_x, height); // single line for half units (m/s)
            }
        }
      line_y = get_y((float) 0);           // draw baseline
      g.drawLine(0, line_y, width, line_y);
      line_y += 1;
      g.drawLine(0, line_y, width, line_y);
    }

  void draw_time_contours(Graphics g)
    {
      int line_x, line_y;
      for (int i=time_start; i < time_start+IGCgraph.GRAPHTIME; i += 3600)
        {
          line_x = time_to_x(i);
          g.drawLine(line_x, 0, line_x, height);
          line_x +=1;
          g.drawLine(line_x, 0, line_x, height); // double line for hours
          for (int j=600; j<=3000; j+=600)        // draw 10 min lines
            { line_x = time_to_x(i+j);
              g.drawLine(line_x, 0, line_x, height);
            }
        }
    }

  void draw_dist_contours(Graphics g)
    {
      int line_x, line_y;
      for (int i=0; i<(IGCgraph.ALTMAXDIST*frame.ApIGCview.config.convert_task_dist); i += 100)
        {
          line_x = dist_to_x((float)i/frame.ApIGCview.config.convert_task_dist);
          g.drawLine(line_x, 0, line_x, height);
          line_x +=1;
          g.drawLine(line_x, 0, line_x, height); // double line for 100km
          for (int j=10; j<=90; j+=10)           // draw 10 km lines
            { line_x = dist_to_x((float)(i+j)/frame.ApIGCview.config.convert_task_dist);
              g.drawLine(line_x, 0, line_x, height);
            }
        }
    }

  public int time_to_x(int time)
    {
      return (int) ((float) (time-time_start) * xscale);
    } 

  public int dist_to_x(float dist)
    {
      return (int) ((dist+IGCgraph.DISTSTART) * xscale);
    } 

  public int box_to_x(int box)
    {
      return climb_to_x((float) IGCgraph.CLIMBBOX * (box - (float) 0.5));
    } 

  public int climb_to_x(float climb)
    {
      return (int) (climb * xscale + IGCgraph.BASELINE);
    }

  int index_to_x(int i)
    {
	// debug:
	//MsgBox m1 = new MsgBox("index_to_x: entry: "+i);
	//if (i==0) return 0;
      return log_index_to_x(frame.ApIGCview.logs[frame.ApIGCview.primary_index], i);
    }

  int log_index_to_x(TrackLog log, int i)
    {
      int next_tp = 1;
      float tp_dist = (float)0.0;

	//MsgBox m1 = new MsgBox("log_index_to_x: entry: "+i);

      if (x_axis()==IGCgraph.TIMEX) return time_to_x(log.time[i]);
      while (next_tp<=log.tps_rounded &&
             log.tp_index[next_tp]<=i &&
             next_tp<frame.ApIGCview.task.tp_count) 
        {
          next_tp++;
          tp_dist += frame.ApIGCview.task.dist[next_tp];
        }
      return dist_to_x(tp_dist - frame.ApIGCview.dec_to_dist(log.latitude[i],
                                                     log.longitude[i], 
                                                     frame.ApIGCview.task.tp[next_tp].latitude,
                                                     frame.ApIGCview.task.tp[next_tp].longitude));
    }

  public int get_y(float y_value)
    {
      return height - (int)((y_value-min_y) * yscale) - IGCgraph.BASELINE;
    }

  public int x_to_time(int x)
    {
      return (int) ((float) x / xscale  + time_start);
    } 

  public float x_to_dist(int x)
    {
      return ((float) x / xscale - IGCgraph.DISTSTART);
    } 

  // MOUSE EVENT HANDLERS: MouseListener

  public void mousePressed (MouseEvent e)
    {
      int time;
      float dist;
      int x = e.getX();
      int i;
      Graphics g = getGraphics();
      draw_cursor(g,cursor1_x); // erase previous cursors
      if (cursor2_x!=cursor1_x) draw_cursor(g, cursor2_x); 
      if (x_axis()==IGCgraph.TIMEX)
        {
          frame.ApIGCview.logs[frame.ApIGCview.primary_index].move_cursor1_time(x_to_time(x));
        }
      else
        {
	  frame.ApIGCview.logs[frame.ApIGCview.primary_index].move_cursor1_dist(x_to_dist(x));
        }
      if (frame.ApIGCview.cursor1<frame.ApIGCview.logs[frame.ApIGCview.primary_index].record_count)
        i = frame.ApIGCview.cursor1+1;
      else
        i = frame.ApIGCview.logs[frame.ApIGCview.primary_index].record_count;
      frame.ApIGCview.canvas.move_cursor2(i);
      draw_cursors(g);
    }

  public void mouseReleased (MouseEvent e) {};
  public void mouseClicked(MouseEvent e) {};
  public void mouseEntered(MouseEvent e) {};
  public void mouseExited(MouseEvent e) {};

  // MOUSE EVENT HANDLERS: MouseMotionListener

  public void mouseDragged (MouseEvent e)
    {
      Graphics g = getGraphics();
      if (cursor2_x!=cursor1_x) draw_cursor(g, cursor2_x); // erase previous cursor2
      cursor2_x = e.getX();
      if (cursor2_x!=cursor1_x)  draw_cursor(g, cursor2_x);        // draw new cursor2
      if (x_axis()==IGCgraph.TIMEX)
        {
          frame.ApIGCview.logs[frame.ApIGCview.primary_index].move_cursor2_time(x_to_time(cursor2_x));
        }
      else
        {
	  frame.ApIGCview.logs[frame.ApIGCview.primary_index].move_cursor2_dist(x_to_dist(cursor2_x));
        }
    }
 
  public void mouseMoved(MouseEvent e) {};


  void draw_cursors(Graphics g)
    {
	if (frame.ApIGCview.cursor1==0)
	  {
	    return;
	  }
      cursor1_x = index_to_x(frame.ApIGCview.cursor1);
    }

  void draw_cursor(Graphics g, int x)
    {
      g.setColor(Color.white);
      g.setXORMode(frame.ApIGCview.config.CURSORCOLOUR.colour);
      g.drawLine(x,0,x,height); // draw new cursor
      g.setPaintMode();
    }

  public void mark_plane(Graphics g, int log_num, int x, int y, int h) // draw plane at x,y
                                                                // with stick length h
    {
	Color c = frame.ApIGCview.config.TRACKCOLOUR[frame.ApIGCview.config.FCOLOUR[log_num][0]];
        g.setColor(Color.white);
        g.setXORMode(c);
        g.fillOval(x-4,y-4,8,8);
        g.setColor(c);
        g.setXORMode(frame.ApIGCview.config.TRACKCOLOUR[frame.ApIGCview.config.FCOLOUR[log_num][1]]);
        g.fillOval(x-2,y-2,4,4);
    }

  // RacingCanvas interface:

  public void mark_time(Graphics g, int time) // display current time on canvas during maggot race
    {
        g.setColor(Color.yellow);
        g.setPaintMode();
        g.fillRect(65,3,60,15);
        g.setColor(Color.black);
	g.drawString(IGCview.format_clock(time), 70, 14);
    }

  public void mark_icon(Graphics g, int log_num, int x, int y, int h, float track)
    {
      mark_plane(g,log_num,x,y,h);
    }           

  public void draw_plane(Graphics g, int log_num, int i) // draw plane at log index i
    { 
      int x = log_index_to_x(frame.ApIGCview.logs[log_num],i);
      int y = get_y(frame.ApIGCview.logs[log_num].altitude[i]);
      mark_plane(g, log_num, x, y, 0);
    }

  public void draw_legend(Graphics g)
    {
      int legend_y = 10;
      TrackLog log;
      for (int i=1; i<=frame.ApIGCview.log_count; i++)
	{
	  if (i==frame.ApIGCview.primary_index || frame.ApIGCview.secondary[i])
	    {
	    	log = frame.ApIGCview.logs[i];
            mark_plane(g,i,10,legend_y,0);
		g.setColor(Color.black);
		g.setPaintMode();
		g.drawString(log.name, 15, legend_y+4);
		legend_y += 12;
	    }
	}
    }

  // KeyListener interface:

  public void keyPressed(KeyEvent e)
    {
        graph_key(e.getKeyCode());
    }

  public void keyReleased(KeyEvent e) {;}

  public void keyTyped(KeyEvent e) {;}

  void graph_key(int key)
    {
      if (frame.ApIGCview.primary_index==0) return;
      switch (key)
        {
	  case KeyEvent.VK_RIGHT: 
	  case KeyEvent.VK_SPACE: 
            graph_cursor_right();
            break;
	  case KeyEvent.VK_LEFT:
	  case KeyEvent.VK_BACK_SPACE:
            graph_cursor_left();
            break;
          case KeyEvent.VK_INSERT:
            graph_insert();
            break;
          case KeyEvent.VK_HOME:
          case KeyEvent.VK_S:
            graph_home();
            break;
          case KeyEvent.VK_END:
          case KeyEvent.VK_E:
            graph_end();
            break;
          default:
        }
    }

  void graph_cursor_right()
    {
      Graphics g = getGraphics();
      if (cursor2_x!=cursor1_x) draw_cursor(g, cursor2_x); // erase previous cursor2
      frame.ApIGCview.canvas.move_cursor2(frame.ApIGCview.cursor2+1);
      cursor2_x = index_to_x(frame.ApIGCview.cursor2);
      if (cursor1_x!=cursor2_x) draw_cursor(g, cursor2_x);
    }

  void graph_cursor_left()
    {
      Graphics g = getGraphics();
      if (cursor2_x!=cursor1_x) draw_cursor(g, cursor2_x); // erase previous cursor2
      frame.ApIGCview.canvas.move_cursor2(frame.ApIGCview.cursor2-1);
      cursor2_x = index_to_x(frame.ApIGCview.cursor2);
      if (cursor1_x!=cursor2_x) draw_cursor(g, cursor2_x);
    }

  void graph_insert()
    {
      Graphics g = getGraphics();
      frame.ApIGCview.canvas.move_cursor1(frame.ApIGCview.cursor2);
      frame.ApIGCview.canvas.map_cursor_right();
      draw_cursor(g,cursor1_x); // erase previous cursors
      if (cursor2_x!=cursor1_x) draw_cursor(g, cursor2_x); 
      draw_cursors(g);
    }

  void graph_home()
    {
      Graphics g = getGraphics();
      frame.ApIGCview.canvas.move_cursor1(1);
      draw_cursor(g,cursor1_x);                             // erase previous cursor1
      if (cursor2_x!=cursor1_x) draw_cursor(g, cursor2_x);  // erase previous cursor2
      draw_cursors(g);      
    }

  void graph_end()
    {
      Graphics g = getGraphics();
      TrackLog log = frame.ApIGCview.logs[frame.ApIGCview.primary_index];
      frame.ApIGCview.canvas.move_cursor2(log.record_count-1);
      draw_cursor(g,cursor1_x);                             // erase previous cursor1
      if (cursor2_x!=cursor1_x) draw_cursor(g, cursor2_x);  // erase previous cursor2
      draw_cursors(g);      
    }

}

