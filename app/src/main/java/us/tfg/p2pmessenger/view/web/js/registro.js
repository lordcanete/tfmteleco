const PATRON_BLOQUES = "pagRegistro_bloque";

function pagRegistro_onClickRegistrar () {
    var usuario = $("#pagRegistro_formFieldUser").val();
    var passwd = $("#pagRegistro_formFieldPass").val();
    mostrarBloqueNotificacionCargando();
    javaConnector.registrarUsuario(usuario, passwd);
};

function pagRegistro_onClickIniciarSesion () {
    javaConnector.accederInicioSesion();
};

function pagRegistro_onClickCerrarNotificacionError(){
    ocultarBloqueNotificacionError();
}

function mostrarBloqueNotificacionOK(){
    mostrarBloqueNotificacion("#pagRegistro_bloqueNotificacionOK");    
}

function ocultarBloqueNotificacionOK(){
    ocultarBloqueNotificacion("#pagRegistro_bloqueNotificacionOK");
}

function mostrarBloqueNotificacionCargando(){
    mostrarBloqueNotificacion("#pagRegistro_bloqueNotificacionCargando");    
}

function ocultarBloqueNotificacionCargando(){
    ocultarBloqueNotificacion("#pagRegistro_bloqueNotificacionCargando");
}

function mostrarBloqueNotificacionError(mensaje){
    mostrarBloqueNotificacion("#pagRegistro_bloqueNotificacionError", "#pagRegistro_mensajeNotificacionError", mensaje);    
}

function ocultarBloqueNotificacionError(){
    ocultarBloqueNotificacion("#pagRegistro_bloqueNotificacionError");
}

function ocultarBloques(patron){
    var selectorPatron = "span[id^="+patron+"]";
    ocultarBloqueNotificacion(selectorPatron);
}

function mostrarBloqueNotificacion(idBloque, idUbicacionMensaje = null, mensaje = null){
    if (idUbicacionMensaje != null && mensaje != null){
        $(idUbicacionMensaje).text(mensaje);
    }
    if(typeof PATRON_BLOQUES !== 'undefined' ){
        ocultarBloques(PATRON_BLOQUES);
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
        mostrarBloqueNotificacionError(mensajeError);
    },
    notificarUsuarioCreadoCorrectamente: function(){        
        mostrarBloqueNotificacionOK();
    }
};

function getJsConnector() {
    return jsConnector;
};