var idBloque_notifError = "#pagConectaPastry_bloqueNotificacionError";
var idBloque_textoError = "#pagConectaPastry_mensajeNotificacionError";

function pagConectaPastry_onClickConectar () {
    var ip = $("#pagConectaPastry_formFieldIP").val();
    var puerto = $("#pagConectaPastry_formFieldPort").val();
    javaConnector.conectarPastry(ip, puerto);
};

function pagConectaPastry_onClickCerrarNotificacionError(){
    ocultarBloqueNotificacion(idBloque_notifError);
}
