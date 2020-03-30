var jsConnector = {
    comprobarEstadoCallback: function () {
        javaConnector.comprobarEstado();
    }
};

function getJsConnector() {
    return jsConnector;
};

$(function(){
    javaConnector.iniciarServicio();
});