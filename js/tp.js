//***************************************************************************//
//            TP                                                             //
//***************************************************************************//

function TP(file_rec)
{
  var self = this;
  self.trigraph = '';
  self.full_name = '';
  self.latitude = 0.0;
  self.longitude = 0.0;
  self.index = 0;

  if (file_rec == null)
    return self;
  else if (file_rec.startsWith("C"))
    igc_tp(file_rec);
  else
    gdn_tp(file_rec);

  function igc_tp(igc_rec)
    {
      self.trigraph = igc_rec.substring(18);
      //trigraph=trigraph.toUpperCase();
      self.full_name = igc_rec.substring(18);
      self.latitude = geo.decimal_latlong(igc_rec.substring(1,3),
                                 igc_rec.substring(3,5)+"."+
                                 igc_rec.substring(5,8),
                                 igc_rec.charAt(8));
      self.longitude = geo.decimal_latlong(igc_rec.substring(9,12),
                                  igc_rec.substring(12,14)+"."+
                                  igc_rec.substring(14,17),
                                  igc_rec.charAt(17));
      console.log("Loaded IGC turnpoint:",self.trigraph);
    }

  function gdn_tp(gdn_rec)
    {
      trigraph = gdn_rec.substring(3,9).toUpperCase();
      full_name = gdn_rec.substring(60);
      latitude = geo.decimal_latlong(gdn_rec.substring(11,13),
                                 gdn_rec.substring(14,21),
                                 gdn_rec.charAt(10));
      longitude = geo.decimal_latlong(gdn_rec.substring(23,26),
                                  gdn_rec.substring(27,34),
                                  gdn_rec.charAt(22));
      console.log("Loaded Gardown turnpoint:",self.trigraph);
    }

  self.equals = function(t) // turnpoint equality
    {
      if (t.latitude != latitude) return false;
      if (t.longitude != longitude) return false;
      return true;
    }

  return self;
} // end of class TP

