var idCapaAgenda ="#pagAppWindow_capaAgenda"
var idCapaAbrirConversacion ="#pagAppWindow_capaAbrirConversacion"
var idListaConversaciones = "#pagAppWindow_bloqueIzquierdoConversaciones"
var idListaMensajesConversacion = "pagAppWindow_bloqueConversacionListaMensajes"

function mostrarCapa(idCapa){
    /*Parche para evitar que salga el scrollbar en JavaFX browser*/
    $(idListaConversaciones).removeClass("overflow-auto");
    $(idListaMensajesConversacion).removeClass("overflow-auto");
    $(idCapa).addClass("d-flex");
    $(idCapa).removeClass("d-none");
}

function ocultarCapa(idCapa){
    $(idCapa).addClass("d-none");
    $(idCapa).removeClass("d-flex");
    /*Parche para evitar que salga el scrollbar en JavaFX browser*/
    $(idListaConversaciones).addClass("overflow-auto");
    $(idListaMensajesConversacion).addClass("overflow-auto");
}

function pagAppWindow_ocultarCapaAgenda(){
    ocultarCapa(idCapaAgenda);    
}

function pagAppWindow_mostrarCapaAgenda(){
    mostrarCapa(idCapaAgenda);   
}

function pagAppWindow_ocultarCapaAbrirConversacion(){
    ocultarCapa(idCapaAbrirConversacion);    
}
function pagAppWindow_mostrarCapaAbrirConversacion(){
    mostrarCapa(idCapaAbrirConversacion);   
}

function pagAppWindow_onClickCerrarSesion () {
    javaConnector.cerrarSesion();
};
