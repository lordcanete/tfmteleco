var idBloque_notifError = "#pagInicioSesion_bloqueNotificacionError";
var idBloque_textoError = "#pagInicioSesion_mensajeNotificacionError";

function pagInicioSesion_onClickAutenticar () {
    var usuario = $("#pagInicioSesion_formFieldUser").val();
    var passwd = $("#pagInicioSesion_formFieldPass").val();    
    javaConnector.iniciarSesion(usuario, passwd);
};

function pagInicioSesion_onClickRegistrar () {
    javaConnector.accederRegistroUsuario();
};
function pagInicioSesion_onClickCerrarNotificacionError(){
    ocultarBloqueNotificacion(idBloque_notifError);
}
