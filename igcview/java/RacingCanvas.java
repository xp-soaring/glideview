//***************************************************************************//
//            RacingCanvas                                                   //
//***************************************************************************//

import java.awt.*;

interface RacingCanvas
{
  public void mark_time(Graphics g, int time); // display current time on canvas during maggot race
  public void draw_plane(Graphics g, int log_num, int i); // draw plane at log index i
  public void mark_plane(Graphics g, int log_num, int x, int y, int h); // draw plane at x,y
                                                                // with stick length h
  public void mark_icon(Graphics g, int log_num, int x, int y, int h, float track); // draw plane at x,y
                                                                // with stick length h
  public void draw_legend(Graphics g);
}
