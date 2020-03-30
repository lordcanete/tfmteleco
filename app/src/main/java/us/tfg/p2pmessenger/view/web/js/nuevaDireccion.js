function pagConectaPastry_onClickConectar () {
    var ip = $("#pagConectaPastry_formFieldIP").val();
    var puerto = $("#pagConectaPastry_formFieldPort").val();
    javaConnector.conectarPastry(ip, puerto);
};

var jsConnector = {
    errorAlertCallback: function (error) {
        $("#errorFormDireccionPastry").text(error);
        $("#errorFormDireccionPastry").removeClass("d-none");
    },
    comprobarEstadoCallback: function () {
        javaConnector.comprobarEstado();
    }

};

function getJsConnector() {
    return jsConnector;
};