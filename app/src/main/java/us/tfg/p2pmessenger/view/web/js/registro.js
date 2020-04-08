var idBloque_notifError = "#pagRegistro_bloqueNotificacionError";
var idBloque_notifOk = "#pagRegistro_bloqueNotificacionOK";
var idBloque_textoError = "#pagRegistro_mensajeNotificacionError";


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