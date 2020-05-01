var idBloque_notifError = "#pagConectaPastry_bloqueNotificacionError";
var idBloque_textoError = "#pagConectaPastry_mensajeNotificacionError";
var mensaje_validacionKO = "Por favor, proporcione una IP y puerto en formato válido"

function pagConectaPastry_onClickConectar () {
    var ip = $("#pagConectaPastry_formFieldIP").val();
    var puerto = $("#pagConectaPastry_formFieldPort").val();
    if(validarFormularioConectaPastry(ip, puerto)){
        javaConnector.conectarPastry(ip, puerto);
    }else{
        mostrarBloqueNotificacion(idBloque_notifError, idBloque_textoError, mensaje_validacionKO);
    }
    
};

function pagConectaPastry_onClickCerrarNotificacionError(){
    ocultarBloqueNotificacion(idBloque_notifError);
}

function validarFormularioConectaPastry(ip, puerto){
    var validacion = false;
    //Fuente validacion IP: https://www.w3resource.com/javascript/form/ip-address-validation.php    
    var ipformat = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
    if(ipformat.test(ip) && (puerto>=0 && puerto<=65535))
    {        
        validacion = true;
    }
    return validacion;

}

function onPageReady(){
    
}