function pagInicioSesion_onClickAutenticar () {
    javaConnector.cargarPagina("../../app/src/main/java/us/tfg/p2pmessenger/view/web/html/appWindow.html");
};

function pagInicioSesion_onClickRegistrar () {
    javaConnector.cargarPagina("../../app/src/main/java/us/tfg/p2pmessenger/view/web/html/registro.html");
};

var jsConnector = {
    showResult: function (result) {
        document.getElementById('result').innerHTML = result;
    }
};

function getJsConnector() {
    return jsConnector;
};