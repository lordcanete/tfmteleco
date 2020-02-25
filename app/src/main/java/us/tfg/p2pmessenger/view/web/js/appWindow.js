var idCapaAgenda ="#pagAppWindow_capaAgenda"
var idListaConversaciones = "#pagAppWindow_bloqueIzquierdoConversaciones"
var idListaMensajesConversacion = "pagAppWindow_bloqueConversacionListaMensajes"
function ocultarCapaAgenda(){
    $(idCapaAgenda).addClass("d-none");
    $(idCapaAgenda).removeClass("d-flex");
    /*Parche para evitar que salga el scrollbar en JavaFX browser*/
    $(idListaConversaciones).addClass("overflow-auto");
    $(idListaMensajesConversacion).addClass("overflow-auto");
}

function mostrarCapaAgenda(){
    /*Parche para evitar que salga el scrollbar en JavaFX browser*/
    $(idListaConversaciones).removeClass("overflow-auto");
    $(idListaMensajesConversacion).removeClass("overflow-auto");
    $(idCapaAgenda).addClass("d-flex");
    $(idCapaAgenda).removeClass("d-none");
}