var idBloque_notifError = "#errorFormDireccionPastry";
var idBloque_textoError = "#errorFormDireccionPastry";

function pagConectaPastry_onClickConectar () {
    var ip = $("#pagConectaPastry_formFieldIP").val();
    var puerto = $("#pagConectaPastry_formFieldPort").val();
    javaConnector.conectarPastry(ip, puerto);
};

function pagConectaPastry_onClickCerrarNotificacionError(){
    ocultarBloqueNotificacion(idBloque_notifError);
}
