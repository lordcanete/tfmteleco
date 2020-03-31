function pagInicioSesion_onClickAutenticar () {
    
};

function pagInicioSesion_onClickRegistrar () {
    javaConnector.accederRegistroUsuario();
};

var jsConnector = {
    comprobarEstadoCallback: function () {
        javaConnector.comprobarEstado();
    }
};

function getJsConnector() {
    return jsConnector;
};