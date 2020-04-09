var idCapaAgenda ="#pagAppWindow_capaAgenda"
var idCapaAbrirConversacion ="#pagAppWindow_capaAbrirConversacion"
var idListaConversaciones = "#pagAppWindow_bloqueIzquierdoConversaciones"
var idListaMensajesConversacion = "pagAppWindow_bloqueConversacionListaMensajes"
var idBloque_notifError = "#pagAppWindow_bloqueNotificacionError";
var idBloque_textoError = "#pagAppWindow_mensajeNotificacionError";

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

function pagAppWindow_onClickNuevaConversacion(){

}

function pagAppWindow_onClickAbrirAgenda(){
    var contactos = javaConnector.obtenerListaContactos();    
    var myJSON = JSON.parse(contactos);
    mostrarBloqueNotificacion(idBloque_notifError, idBloque_textoError, JSON.stringify(myJSON));
}

function pagAppWindow_onClickCerrarNotificacionError(){
    ocultarBloqueNotificacion(idBloque_notifError);
}