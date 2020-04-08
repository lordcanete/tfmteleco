var idBloque_notifError = "#pagRegistro_bloqueNotificacionError";
var idBloque_notifOk = "#pagRegistro_bloqueNotificacionOK";
var idBloque_textoError = "#pagRegistro_mensajeNotificacionError";
var mensaje_validacionKO = "Por favor, proporcione un usuario y una contraseña. Además, el campo \"Confirmar contraseña\" debe coincidir con el campo \"Contraseña\""


function pagRegistro_onClickRegistrar () {    
    var usuario = $("#pagRegistro_formFieldUser").val();
    var passwd = $("#pagRegistro_formFieldPass").val();    
    var passwdConfirm = $("#pagRegistro_formFieldPassConfirm").val();
    if(validarFormularioRegistroUsuario(usuario, passwd, passwdConfirm)){
        javaConnector.registrarUsuario(usuario, passwd);
    }else{
        mostrarBloqueNotificacion(idBloque_notifError, idBloque_textoError, mensaje_validacionKO);
    }
    
};

function pagRegistro_onClickIniciarSesion () {
    javaConnector.accederInicioSesion();
};

function pagRegistro_onClickCerrarNotificacionError(){
    ocultarBloqueNotificacion(idBloque_notifError);
}

function validarFormularioRegistroUsuario(usuario, passwd, passwdConfirm){
    var validacion = false;
    if ((!(usuario.trim() == "") && !(passwd.trim() == "")) && (passwd == passwdConfirm)) {
        validacion = true;
    } 
    return validacion;
}