//***************************************************************************//
//            Climb                                                          //
//***************************************************************************//

function Climb()
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