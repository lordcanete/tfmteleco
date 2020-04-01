function pagRegistro_onClickRegistrar () {
    
};

function pagRegistro_onClickIniciarSesion () {
    javaConnector.accederInicioSesion();
};

var jsConnector = {
    comprobarEstadoCallback: function () {
        javaConnector.comprobarEstado();
    }
};

function getJsConnector() {
    return jsConnector;
};