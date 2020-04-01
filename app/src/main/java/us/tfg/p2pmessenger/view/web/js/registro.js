function pagRegistro_onClickRegistrar () {
    var usuario = $("#pagRegistro_formFieldUser").val();
    var passwd = $("#pagRegistro_formFieldPass").val();
    javaConnector.registrarUsuario(usuario, passwd);
};

function pagRegistro_onClickIniciarSesion () {
    javaConnector.accederInicioSesion();
};

function pagRegistro_onClickCerrarNotificacionError(){
    ocultarBloqueNotificacionError();
}

function mostrarBloqueNotificacionOK(){
    $("#pagRegistro_bloqueNotificacionOK").removeClass("d-none");
}

function ocultarBloqueNotificacionOK(){
    $("#pagRegistro_bloqueNotificacionOK").addClass("d-none");
}

function mostrarBloqueNotificacionError(mensaje){
    $("#pagRegistro_mensajeNotificacionError").text(mensaje);
    $("#pagRegistro_bloqueNotificacionError").removeClass("d-none");
}

function ocultarBloqueNotificacionError(){
    $("#pagRegistro_bloqueNotificacionError").addClass("d-none");
}

var jsConnector = {
    comprobarEstadoCallback: function () {
        javaConnector.comprobarEstado();
    },
    notificarError: function(mensajeError){
        mostrarBloqueNotificacionError(mensajeError);
    },
    notificarUsuarioCreadoCorrectamente: function(){
        mostrarBloqueNotificacionOK();
    }
};

function getJsConnector() {
    return jsConnector;
};