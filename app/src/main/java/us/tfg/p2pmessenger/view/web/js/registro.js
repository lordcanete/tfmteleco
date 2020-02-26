function pagRegistro_onClickRegistrar () {
    javaConnector.cargarPagina("../../app/src/main/java/us/tfg/p2pmessenger/view/web/html/inicioSesion.html");
};

function pagRegistro_onClickIniciarSesion () {
    javaConnector.cargarPagina("../../app/src/main/java/us/tfg/p2pmessenger/view/web/html/inicioSesion.html");
};

var jsConnector = {
    showResult: function (result) {
        document.getElementById('result').innerHTML = result;
    }
};

function getJsConnector() {
    return jsConnector;
};