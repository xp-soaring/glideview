// igcview.js
// author: Ian Forster-Lewis

// this JavaScript code is mostly called from the IGCview java applet, to get HTML pop-ups, etc. to
//  appear.  The tp_info and task_info windows are now HTML, and they call this code also.

// 2003-05-13 added code in show_tp_info to parseInt tp_index for Sun Java plugin compatibility

// These functions are called from the IGCview applet, to interact with the browser.
// They could easily be improved, with only XML coming from the applet.  Whatever.


// ---------------------------------------------------------------------------------------------------

// igc_cursor_data(s): display the cursor data in the left frame
function igc_cursor_data(s)
  {
    top.left_frame.document.open();
    top.left_frame.document.write(s);
    top.left_frame.document.close();
  }

// igc_ruler_data(s): display the ruler data in the left frame
function igc_ruler_data(s)
  {
    top.left_frame.document.open();
    top.left_frame.document.write(s);
    top.left_frame.document.close();
  }

// igc_task_data(s): display the task data in the left frame
function igc_task_data(s)
  {
    top.left_frame.document.open();
    top.left_frame.document.write(s);
    top.left_frame.document.close();
  }

// igc_scoring_data(s): write the scoring data into a new window
function igc_scoring_data(s)
  {
    var new_win = window.open("", "IGCview - Scoring summary");
    new_win.document.open();
    new_win.document.write(s);
    new_win.document.close();
  }
    
// igc_flight_data(s): write the scoring data into a new window
function igc_flight_data(s)
  {
    new_win = window.open("", "DataWindow", "menubar=no,toolbar=no,resizable=yes,scrollbars=yes");
    new_win.document.open();
    new_win.document.write(s);
    new_win.document.close();
  }

// igc_security_error(s): open a new window with help info, as file dialog open failed
function igc_security_error(s)
  {
    window.open("documentation/install/security_error.html");
  }

// igc_save_prefs(prefs) will save string preferences in a cookie
function igc_save_prefs(prefs)
  {
  	var expdate = new Date();
        expdate.setTime(expdate.getTime() + (1000 * 60 * 60 * 24 * 365 * 10)); // expire in 10 years
	  document.cookie = 'igcview_prefs=' + escape(prefs) + '; expires=' + expdate.toGMTString() + ';'
  }

// igc_load_prefs() will load preferences from cookie
function igc_load_prefs()
  {
	  if (document.cookie == '') return ''; // no cookie set
  	var firstChar, lastChar
  	var theBigCookie = document.cookie;
    var name = 'igcview_prefs';
	  firstChar = theBigCookie.indexOf(name+'=');
		if(firstChar == -1) return ''; // igcview_prefs not set in cookie

  	firstChar += name.length + 1; // skip 'name' and '='
	 	lastChar = theBigCookie.indexOf(';', firstChar);
  	if (lastChar == -1) lastChar = theBigCookie.length;	    
  	return unescape(theBigCookie.substring(firstChar, lastChar));
  }

// igc_querystring(key) will return the value associated with the key in the querystring,
//   e.g. if IGCview is run with  http://igcview.com/igcview.html?log_file=abcd.igc
//   then igc_querystring("log_file") will return "abcd.igc"
// querystring values are log_file, log_list, tp_file, map_list

function igc_querystring(key)
  {
  	var query = unescape(window.top.location.search.substring(1));
    var begin = query.indexOf(key+"=");
    if (begin<0) return "";
    begin = begin + key.length + 1;    // begin points to start of value
    var end = query.indexOf("&",begin);
    if (end<0) end = query.length; // end points to end of value
    if (begin >= end)
      return ""
    else
      return query.substring(begin, end);
  }

// ******************************************************************* //    
// a couple of utility functions for message boxes and status messages

// display an "OK / Cancel" Message Box
function igc_msgbox(s)
  {
    alert(s);
  }

// display a "Yes / No / Cancel" Message Box
function igc_confirm(s)
  {
    return confirm(s);
  }

// show text in the browser status bar
function igc_status(s)
  {
    window.status = s;
  }

  //***********************************************************************************************************//
//**********   Code to handle display of TP info window                                        ************//
//***********************************************************************************************************//

// igc_show_tp_info(tp) is called from the IGCview applet to pop up a turnpoint detail window
// tp is the index of the turnpoint in the IGCview tp_db internal turnpoint array

var tp_window; // handle to window showing turnpoint detail
var tp_index;  // index of current turnpoint in IGCview internal database (tp_db)
var task_tp = false; // igc_show_tp_info will display a TP of the current task rather than any TP from database

function igc_show_task_tp_info(tp)
  {
    // task_tp = true;
    // show_tp(tp);
    igc_show_task_info(); // currently show task if task tp clicked on
  }
  
function igc_show_tp_info(tp)
  {
    task_tp = false;
    show_tp(tp);
  }
  
function show_tp(tp)
  {
    tp_window = null;
    tp_index = parseInt(""+tp);
    window.open("tp_info.html","tp_info", "width=290,height=220,resizable=yes,scrollbars=yes");
    // if (tp_window.opener==null) tp_window.opener = self;
    //   tp_window.focus();
  }

// save_tp_info() is called by the tp detail pop-up window when the user clicks the 'save' button

function save_tp_info()
  {
    // next lines commented out as task tp info displayed in a previous version
    // parent.right_frame.igcview.igc_set_tp_data(tp_index,"add_to_task",tp_window.document.tp_info.add_to_task.value);
    // var area = tp_window.document.tp_info.tp_or_area[1].checked;
    parent.right_frame.igcview.igc_set_tp_data(tp_index,"trigraph",tp_window.document.tp_info.trigraph.value);
    parent.right_frame.igcview.igc_set_tp_data(tp_index,"full_name",tp_window.document.tp_info.full_name.value);
    parent.right_frame.igcview.igc_set_tp_data(tp_index,"latitude",tp_window.document.tp_info.latitude.value);
    parent.right_frame.igcview.igc_set_tp_data(tp_index,"longitude",tp_window.document.tp_info.longitude.value);
    
    /* Remove Task TP info that was displayed in an earlier version of tp_info
    parent.right_frame.igcview.igc_set_tp_data(tp_index,"tp_or_area", (area ? "area" : "tp"));
    if (area)
      {
        parent.right_frame.igcview.igc_set_tp_data(tp_index,"radius1",tp_window.document.tp_info.radius1.value);
        parent.right_frame.igcview.igc_set_tp_data(tp_index,"radius2",tp_window.document.tp_info.radius2.value);
        parent.right_frame.igcview.igc_set_tp_data(tp_index,"bearing1",tp_window.document.tp_info.bearing1.value);
        parent.right_frame.igcview.igc_set_tp_data(tp_index,"bearing2",tp_window.document.tp_info.bearing2.value);
      }
    */
    parent.right_frame.igcview.igc_set_tp_data(tp_index,"done",""); // signal tp update as completed        
    // alert("Turnpoint information for "+tp_window.document.tp_info.trigraph.value+" saved.");
  }

// load_tp_info() is called by the tp detail pop-up window when it opens, to load the data to appear in that window

function load_tp_info(w)
  {
    tp_window = w;
    if (tp_window==null || tp_window.document==null) // popup not fully open yet...
      {
        alert("IGCview error: load_tp_info(). TP info window is unavailable.");
        return;
      }

    // tp_window initialized, now go load the data
    tp_window.document.tp_info.trigraph.value        = parent.right_frame.igcview.igc_get_tp_data(tp_index,"trigraph");
    tp_window.document.tp_info.full_name.value       = parent.right_frame.igcview.igc_get_tp_data(tp_index,"full_name");
    tp_window.document.tp_info.latitude.value        = parent.right_frame.igcview.igc_get_tp_data(tp_index,"latitude");
    tp_window.document.tp_info.longitude.value       = parent.right_frame.igcview.igc_get_tp_data(tp_index,"longitude");

    tp_window.focus();
  }

//***********************************************************************************************************//
//**********   End of code to handle display of TP detail window                                 ************//
//***********************************************************************************************************//

//***********************************************************************************************************//
//**********   Code to handle display of Task info window                                        ************//
//***********************************************************************************************************//

var task_window; // handle to window showing task information

function igc_show_task_info()
  {
    task_window = null;
    // task_window = window.open("task_info.html","task_info", "width=740,height=480,resizable=yes,scrollbars=yes");
    window.open("task_info.html","task_info", "width=740,height=480,resizable=yes,scrollbars=yes");
    // if (task_window.opener==null) task_window.opener = self;
  }

// load_task_info() is called by the task info pop-up window when it opens, to load the data to appear in that window

function load_task_info(w)
  {
    task_window = w;
    if (task_window==null || task_window.document==null) // popup not fully open yet...
      {
        alert("IGCview error: load_task_info(). Task information window not available");
        return;
      }
    var tp_count = parseInt(parent.right_frame.igcview.igc_get_task_data(1,"tp_count"));
    task_window.set_value(1,"tp_count",""+tp_count);
    var dist_units = parent.right_frame.igcview.igc_get_task_data(1,"dist_units");
    task_window.set_value(1,"dist_units",dist_units);
    
    var i, trigraph, full_name, dist, track, tp_or_area, radius1, radius2, bearing1, bearing2, dist_units ;
    for (i=1; i<=tp_count; i++)
      {
        trigraph = parent.right_frame.igcview.igc_get_task_data(i,"trigraph");
        task_window.set_value(i,"trigraph",trigraph);
        full_name = parent.right_frame.igcview.igc_get_task_data(i,"full_name");
        task_window.set_value(i,"full_name",full_name);
        if (i>1)
          {
            dist = parent.right_frame.igcview.igc_get_task_data(i,"dist");
            task_window.set_value(i,"dist",dist);
            track = parent.right_frame.igcview.igc_get_task_data(i,"track");
            task_window.set_value(i,"track",track);
          }
        tp_or_area = parent.right_frame.igcview.igc_get_task_data(i,"tp_or_area");
        task_window.set_value(i,"tp_or_area",tp_or_area);
        radius1 = parent.right_frame.igcview.igc_get_task_data(i,"radius1");
        task_window.set_value(i,"radius1",radius1);
        radius2 = parent.right_frame.igcview.igc_get_task_data(i,"radius2");
        task_window.set_value(i,"radius2",radius2);
        bearing1 = parent.right_frame.igcview.igc_get_task_data(i,"bearing1");
        task_window.set_value(i,"bearing1",bearing1);
        bearing2 = parent.right_frame.igcview.igc_get_task_data(i,"bearing2");
        task_window.set_value(i,"bearing2",bearing2);
      }
    if (tp_count>1)
      {
        var length = parent.right_frame.igcview.igc_get_task_data(0,"length");
        task_window.set_value(0,"length",length);
      }
    task_window.focus();
  }
  
function save_task_info()
  {
    var tp_count = task_window.get_tp_count();
    var i;
    for (i=1; i<=tp_count; i++)
      {
        parent.right_frame.igcview.igc_set_task_data(i,"tp_or_area",task_window.get_value(i,"tp_or_area"));
        parent.right_frame.igcview.igc_set_task_data(i,"radius1",task_window.get_value(i,"radius1"));
        parent.right_frame.igcview.igc_set_task_data(i,"radius2",task_window.get_value(i,"radius2"));
        parent.right_frame.igcview.igc_set_task_data(i,"bearing1",task_window.get_value(i,"bearing1"));
        parent.right_frame.igcview.igc_set_task_data(i,"bearing2",task_window.get_value(i,"bearing2"));
      }
    parent.right_frame.igcview.igc_set_task_data(1,"done",""); // signal task update as completed        
  }

function search_tps(key)
  {
    return parent.right_frame.igcview.igc_search_tps(key);
  }

// insert new turnpoint with tp_index in task at position tp_num
function insert_task_tp(tp_num, tp_index)
  {
    parent.right_frame.igcview.igc_insert_task_tp(tp_num,tp_index); 
    task_window.refresh();
    parent.right_frame.igcview.igc_paint_map(); // refresh map window            
  }

// change turnpoint in task at position tp_num with new tp with index tp_index
function change_task_tp(tp_num, tp_index)
  {
    parent.right_frame.igcview.igc_change_task_tp(tp_num,tp_index);        
    task_window.refresh();       
    parent.right_frame.igcview.igc_paint_map(); // refresh map window            
  }

// delete turnpoint in task at position tp_num 
function delete_task_tp(tp_num)
  {
    parent.right_frame.igcview.igc_delete_task_tp(tp_num);            
    task_window.refresh();       
    parent.right_frame.igcview.igc_paint_map(); // refresh map window            
  }
  
//***********************************************************************************************************//
//**********   End of code to handle display of task info window                                 ************//
//***********************************************************************************************************//




