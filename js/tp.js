//***************************************************************************//
//            TP                                                             //
//***************************************************************************//

function TP()
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

