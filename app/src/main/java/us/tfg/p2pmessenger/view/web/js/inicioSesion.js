var idBloque_notifError = "#pagInicioSesion_bloqueNotificacionError";
var idBloque_textoError = "#pagInicioSesion_mensajeNotificacionError";
var mensaje_validacionKO = "Por favor, proporcione un usuario y una contraseña"

async function pagInicioSesion_onClickAutenticar () {
    var usuario = $("#pagInicioSesion_formFieldUser").val();
    var passwd = $("#pagInicioSesion_formFieldPass").val();        
    if(validarFormularioInicioSesion(usuario, passwd)){
        mostrarBloque(idCapaCargando);
        await sleep(100);
        javaConnector.iniciarSesion(usuario, passwd);
    }else{
        mostrarBloqueNotificacion(idBloque_notifError, idBloque_textoError, mensaje_validacionKO);
    }
};

async function pagInicioSesion_onClickRegistrar () {
    mostrarBloque(idCapaCargando);
    await sleep(100);
    javaConnector.accederRegistroUsuario();
};
function pagInicioSesion_onClickCerrarNotificacionError(){
    ocultarBloqueNotificacion(idBloque_notifError);
}

function validarFormularioInicioSesion(usuario, passwd){
    var validacion = false;
    if (!(usuario.trim() == "") && !(passwd.trim() == "")) {
        validacion = true;
    } 
    return validacion;
}
