var idBloque_notifError = "#pagRegistro_bloqueNotificacionError";
var idBloque_notifOk = "#pagRegistro_bloqueNotificacionOK";
var idBloque_textoError = "#pagRegistro_mensajeNotificacionError"


function pagRegistro_onClickRegistrar () {    
    var usuario = $("#pagRegistro_formFieldUser").val();
    var passwd = $("#pagRegistro_formFieldPass").val();    
    javaConnector.registrarUsuario(usuario, passwd);
};

function pagRegistro_onClickIniciarSesion () {
    javaConnector.accederInicioSesion();
};

function pagRegistro_onClickCerrarNotificacionError(){
    ocultarBloqueNotificacion(idBloque_notifError);
}

function mostrarBloqueNotificacion(idBloque, idUbicacionMensaje = null, mensaje = null){
    if (idUbicacionMensaje != null && mensaje != null){
        $(idUbicacionMensaje).text(mensaje);
    }

    $(idBloque).addClass("d-flex");
    $(idBloque).removeClass("d-none");
}

function ocultarBloqueNotificacion(idBloque){
    $(idBloque).addClass("d-none");
    $(idBloque).removeClass("d-flex");
}

var jsConnector = {
    comprobarEstadoCallback: function () {
        javaConnector.comprobarEstado();
    },
    notificarError: function(mensajeError){
        mostrarBloqueNotificacion(idBloque_notifError, idBloque_textoError, mensajeError);
    },
    notificarUsuarioCreadoCorrectamente: function(){        
        mostrarBloqueNotificacionOK(idBloque_notifOk);
        this.comprobarEstadoCallback();
    }
};

function getJsConnector() {
    return jsConnector;
};