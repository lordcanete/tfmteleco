var idCapaAgenda ="#pagAppWindow_capaAgenda"
var idCapaAbrirConversacion ="#pagAppWindow_capaAbrirConversacion"
var idListaConversaciones = "#pagAppWindow_bloqueIzquierdoConversaciones"
var idListaMensajesConversacion = "pagAppWindow_bloqueConversacionListaMensajes"
var idBloque_notifError = "#pagAppWindow_bloqueNotificacionError";
var idBloque_textoError = "#pagAppWindow_mensajeNotificacionError";

var mockup_jsonContactos = "";

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

function pagAppWindow_mostrarCapaAgenda(listaContactosJson){
    var contactos = JSON.stringify(listaContactosJson);
    /*var panelAgendaListaContactos = $("#pagAppWindow_PanelAgendaListaContactos");
    panelAgendaListaContactos.empty();
    listaContactosJson.forEach(function(contactoJson) { 
        var contactoBoxAgenda = crearContactoBoxAgenda(contactoJson);
        panelAgendaListaContactos.appendChild(contactoBoxAgenda);        
    });*/
    mostrarBloqueNotificacion(idBloque_notifError, idBloque_textoError, contactos);
    //mostrarCapa(idCapaAgenda);   
}

function crearContactoBoxAgenda(contactoJson) {
    var alias = contactoJson.alias;
    var usuario = contactoJson.usuario;
    var plantilla = $("#contactoBoxAgendaPlantilla");
  
    var contactoBoxAgenda = plantilla.cloneNode(true);
    contactoBoxAgenda.id = "contactoBoxAgenda-" + usuario;
    contactoBoxAgenda.removeClass("d-none");

    var aliasElement = contactoBoxAgenda.querySelector(".contactoBoxAlias");
    aliasElement.innerText = alias;    
    var usuarioElement = contactoBoxAgenda.querySelector(".contactoBoxUsuario span");
    usuarioElement.innerText = usuario;   
  
    return contactoBoxAgenda;
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
    javaConnector.obtenerListaContactos();        
}

function pagAppWindow_onClickCerrarNotificacionError(){
    ocultarBloqueNotificacion(idBloque_notifError);
}

