function GlideView()
{

// Start GlideView object instantiated with "new GlideView()"
let self = this;

self.drop_area; // drop area DOM object

self.file_reader; // FileReader to read drag/dropped IGC files

self.progress_bar;

self.init = function()
{
    this.drop_area = document.getElementById("drop_area");

    init_file_reader();

    init_map();

    init_drop_area();

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
    var cambridge = new L.LatLng(52.20038, 0.1197);
    map.setView(cambridge, 15).addLayer(osm); //.addLayer(sites_layer).addLayer(links_layer);
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
}

} // GlideView