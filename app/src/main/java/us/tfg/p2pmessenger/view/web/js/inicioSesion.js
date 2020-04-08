var idBloque_notifError = "#pagInicioSesion_bloqueNotificacionError";
var idBloque_textoError = "#pagInicioSesion_mensajeNotificacionError";
var mensaje_validacionKO = "Por favor, proporcione un usuario y una contraseña"

function pagInicioSesion_onClickAutenticar () {
    var usuario = $("#pagInicioSesion_formFieldUser").val();
    var passwd = $("#pagInicioSesion_formFieldPass").val();        
    if(validarFormularioInicioSesion(usuario, passwd)){
        javaConnector.iniciarSesion(usuario, passwd);
    }else{
        mostrarBloqueNotificacion(idBloque_notifError, idBloque_textoError, mensaje_validacionKO);
    }
};

function pagInicioSesion_onClickRegistrar () {
    javaConnector.accederRegistroUsuario();
};
function pagInicioSesion_onClickCerrarNotificacionError(){
    ocultarBloqueNotificacion(idBloque_notifError);
}

function validarFormularioInicioSesion(usuario, passwd){
    var validacion = false;
    var usuarioTrim = usuario.trim();
    var passwdTrim = passwd.trim();
    if (!(usuario.trim() == "") && !(passwd.trim() == "")) {
        validacion = true;
    } 
    return validacion;
}