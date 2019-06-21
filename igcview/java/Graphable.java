//***************************************************************************//
//            Graphable                                                      //
//***************************************************************************//

interface Graphable
{
  public int x_axis();
  public int time_to_x(int time);
  public int dist_to_x(float dist);
  public int box_to_x(int box);
  public int climb_to_x(float climb);
  public int get_y(float y_value);
}

