function GlideView()
{

// Start GlideView object instantiated with "new GlideView()"
let self = this;

var map_div;
var baro_div;
var task_div;

self.drop_area; // drop area DOM object

self.file_reader; // FileReader to read drag/dropped IGC files

self.progress_bar;

self.tracklogs = new Array();

self.init = function()
{
    map_div = document.getElementById('map');
    baro_div = document.getElementById('baro');
    task_div = document.getElementById('task');

    this.drop_area = document.getElementById("drop_area");

    init_file_reader();

    init_map();

    init_drop_area();

}

self.show_map = function(id)
{
    console.log('showing map',id);

    if (baro_div)
    {
        baro_div.style.display = 'none';
    }
    if (task_div)
    {
        task_div.style.display = "none"
    }
    map_div = document.getElementById(id);
    map_div.style.display = 'block';
}

self.show_baro = function(id)
{
    console.log('showing baro', id);

    if (map_div)
    {
        map_div.style.display = 'none';
    }
    if (task_div)
    {
        task_div.style.display = "none"
    }
    baro_div = document.getElementById(id);
    baro_div.style.display = 'block';
    if (self.tracklogs.length > 0)
    {
        self.tracklogs[0].draw_baro(baro_div);
    }
}

self.show_task = function(id)
{
    if (map_div)
    {
        map_div.style.display = 'none';
    }
    if (baro_div)
    {
        baro_div.style.display = "none"
    }
    task_div = document.getElementById(id);
    task_div.style.display = 'block';
}

// Setup the map environment
function init_map()
{

    map = L.map('map');

    // Various feature layers
    //sites_layer = L.featureGroup();
    //links_layer = L.featureGroup();
    //compound_routes_layer = L.featureGroup();

    // Various map providers
    var osm = L.tileLayer.provider('OpenStreetMap.Mapnik');
    /* var mb = L.tileLayer.provider('MapBox', {
        id: 'mapbox.streets',
        accessToken: MB_ACCESS_TOKEN
    });
    var tf = L.tileLayer.provider('Thunderforest.Neighbourhood', {
        apikey: TF_API_KEY
    });*/

    // Layer control
    var base_layers = {
        //'MapBox': mb,
        //'ThunderForest': tf,
        'OSM': osm,
    };
    //var overlay_layers = {
    //    'Sites': sites_layer,
    //    'All links': links_layer,
    //};
    layer_control = L.control.layers(base_layers).addTo(map);

    // Handler to clear any highlighting caused by clicking lines
    //map.on('click', clear_line_highlight);

    //get_legend().addTo(map);

    // Centre on Cambridge and add default layers
    var map_center = new L.LatLng(config.map_position.latitude, config.map_position.longitude);
    map.setView(map_center, config.map_position.scale).addLayer(osm); //.addLayer(sites_layer).addLayer(links_layer);
} // init_map()

function init_file_reader()
{
    self.progress_bar = document.getElementById("progress_bar");

    self.file_reader = new FileReader();

    self.file_reader.onerror = file_reader_error;

    self.file_reader.onload = file_reader_load;

} // init_file_reader()

function init_drop_area()
{
    function preventDefaults (e) {
        e.preventDefault()
        e.stopPropagation()
    }

    ;['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
        drop_area.addEventListener(eventName, preventDefaults, false)
    })

    ;['dragenter', 'dragover'].forEach(eventName => {
        drop_area.addEventListener(eventName, highlight, false)
    })

    ;['dragleave', 'drop'].forEach(eventName => {
        drop_area.addEventListener(eventName, unhighlight, false)
    })

    function highlight(e) {
        drop_area.classList.add('highlight')
    }

    function unhighlight(e) {
        drop_area.classList.remove('highlight')
    }

    drop_area.addEventListener('drop', handle_drop, false)

} // init_drop_area()

// ********************
// drop_area functions
// ********************

function handle_file(file) {
    console.log("got file",file.name);
    self.file_reader.readAsText(file);
}

self.handle_files = function(files) {
    ([...files]).forEach(handle_file)
}

function handle_drop(e) {
    let dt = e.dataTransfer
    let files = dt.files

    self.handle_files(files)
}

// **********************
// file_reader functions
// **********************

function file_reader_error()
{
    console.log("FileReader error");
}

function file_reader_load()
{
    console.log("FileReader load completed");
    var tracklog = new TrackLog(self.file_reader.result, self.tracklogs.length);
    console.log("Loaded tracklog",tracklog.date)
    document.getElementById("debug").innerHTML = tracklog.task.toHTML();
    tracklog.task.draw(map);
    tracklog.draw(map, {fitBounds: true});
    self.tracklogs.push(tracklog);
}

    return self;
} // GlideView