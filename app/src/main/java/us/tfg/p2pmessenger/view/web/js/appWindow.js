var idCapaAgenda ="#pagAppWindow_capaAgenda"
var idListaConversaciones = "#pagAppWindow_bloqueIzquierdoConversaciones"
var idListaMensajesConversacion = "#pagAppWindow_bloqueConversacionListaMensajes"
var idInput_CrearContactoAlias = "#pagAppWindow_PanelAgendaAgregarContactoFieldAlias"
var idInput_CrearContactoUsuario = "#pagAppWindow_PanelAgendaAgregarContactoFieldIDUsuario"
var idBloque_notifError = "#pagAppWindow_bloqueNotificacionError";
var idBloque_textoError = "#pagAppWindow_mensajeNotificacionError";
var claseBloque_conversacionAbierta = "conversacionBoxAbierta";
var prefijoSelectorClase = ".";
var idBloque_listaConversacionesDefault = "#pagAppWindow_bloqueIzquierdoConversacionesDefault";
var prefijoIdBloque_contactoBoxAgenda = "contactoBoxAgenda-";
var prefijoIdBloque_conversacionBox = "conversacionBox-";
var const_MAXLENGTHMENSAJECONVERSACIONBOX = 20;
var const_PRIMERMENSAJEDEFAULT = 0;
var const_ULTIMOMENSAJEDEFAULT = 20;
var string_PUNTOSSUSPENSIVOS = "..."


var mensaje_validacionCrearContactoKO = "Por favor, rellene los campos de Usuario y Alias";

var mockup_jsonContactos = '[{"alias":"canete2","usuario":"canete2"},{"alias":"canuto","usuario":"canuto"},{"alias":"tarrilla","usuario":"tarrilla"}]';
var mockup_jsonConversaciones = '[{"aliasRemitente":"canuto","ultimoMensaje":"canuto: joe que mal esto sa rayao","fechaUltimoMensaje":1586698750708,"tipo":2,"pendiente":false,"seleccionada":false},{"aliasRemitente":"canutito","ultimoMensaje":"canutito: Hola canete!","fechaUltimoMensaje":1586695475480,"tipo":2,"pendiente":false,"seleccionada":false}]'

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
    pagAppWindow_refrescarContactosAgenda(listaContactosJson);
    mostrarCapa(idCapaAgenda); 
}

function pagAppWindow_refrescarContactosAgenda(listaContactosJson){      
    var panelAgendaListaContactos = $("#pagAppWindow_PanelAgendaListaContactos");
    panelAgendaListaContactos.empty();
    listaContactosJson.forEach(function(contactoJson) { 
        var contactoBoxAgenda = pagAppWindow_crearContactoBoxAgenda(contactoJson);
        panelAgendaListaContactos.append(contactoBoxAgenda);        
    });          
}

function pagAppWindow_crearContactoBoxAgenda(contactoJson) {
    var alias = contactoJson.alias;
    var usuario = contactoJson.usuario;
    var plantilla = $("#contactoBoxAgendaPlantilla");
  
    var contactoBoxAgenda = plantilla.clone(true);
    contactoBoxAgenda.attr("id",prefijoIdBloque_contactoBoxAgenda + usuario);
    contactoBoxAgenda.removeClass("d-none");    

    var aliasElement = contactoBoxAgenda.find(".contactoBoxAlias");
    aliasElement.text(alias);    
    var usuarioElement = contactoBoxAgenda.find(".contactoBoxUsuario span");
    usuarioElement.text(usuario);   
  
    return contactoBoxAgenda;
  }

function pagAppWindow_mostrarAgendaActualizada(){    
    javaConnector.obtenerListaContactos();
}

function pagAppWindow_onClickCerrarSesion () {
    javaConnector.cerrarSesion();
};

function pagAppWindow_onClickAbrirAgenda(){
    //pagAppWindow_mostrarCapaAgenda(JSON.parse(mockup_jsonContactos));
    pagAppWindow_mostrarAgendaActualizada();        
}

function pagAppWindow_onClickCerrarNotificacionError(){
    ocultarBloqueNotificacion(idBloque_notifError);
}

function pagAppWindow_onClickGuardarContacto(){
    var usuario = $("#pagAppWindow_PanelAgendaAgregarContactoFieldIDUsuario").val();
    var alias = $("#pagAppWindow_PanelAgendaAgregarContactoFieldAlias").val();    
    if(pagAppWindow_validarFormularioCrearContacto(usuario, alias)){
        javaConnector.crearContacto(usuario, alias);
    }else{
        mostrarBloqueNotificacion(idBloque_notifError, idBloque_textoError, mensaje_validacionCrearContactoKO);
    }
}

function pagAppWindow_onClickEliminarContacto(item){
    var idContactoBoxAgenda = item.parentElement.parentElement.id;
    var idUsuario = idContactoBoxAgenda.substring(18);
    javaConnector.eliminarContacto(idUsuario);

}

function pagAppWindow_onClickSeleccionarConversacion(item){
    if(!item.classList.contains(claseBloque_conversacionAbierta)){
        var aliasRemitente = item.id.substring(prefijoIdBloque_conversacionBox.length);
        javaConnector.obtenerListaConversacionesAbiertas(aliasRemitente);    
    }
    
}


function pagAppWindow_limpiarFormularioNuevoContacto(){
    limpiarTextoInput(idInput_CrearContactoAlias);
    limpiarTextoInput(idInput_CrearContactoUsuario);
}

function pagAppWindow_validarFormularioCrearContacto(usuario, alias){
    var validacion = false;
    if (!(usuario.trim() == "") && !(alias.trim() == "")) {
        validacion = true;
    } 
    return validacion;
}

function pagAppWindow_refrescarListaConversacionesAbiertas(listaConversacionesJSON){
    var panelListaConversacionesAbiertas = $("#pagAppWindow_bloqueIzquierdoConversaciones");
    var hayConversacionSeleccionada = false;
    panelListaConversacionesAbiertas.find(prefijoSelectorClase + claseBloque_conversacionAbierta).remove();
    if(listaConversacionesJSON.length > 0){
        ocultarBloqueNotificacion(idBloque_listaConversacionesDefault);
        listaConversacionesJSON.forEach(function(conversacionJson) { 
            var conversacionBox = pagAppWindow_crearConversacionBox(conversacionJson);
            panelListaConversacionesAbiertas.append(conversacionBox);      
            if(conversacionJson.seleccionada){
                hayConversacionSeleccionada = true;
            }
        }); 
        if(hayConversacionSeleccionada){
            javaConnector.obtenerMensajesConversacionSeleccionada(const_PRIMERMENSAJEDEFAULT, const_ULTIMOMENSAJEDEFAULT);
        }
    }else{
        mostrarBloqueNotificacion(idBloque_listaConversacionesDefault);
    }
    
}

function pagAppWindow_crearConversacionBox(conversacionJson) {
    var aliasRemitente = conversacionJson.aliasRemitente;
    var ultimoMensaje = conversacionJson.ultimoMensaje;
    var fechaUltimoMensaje = conversacionJson.fechaUltimoMensaje;
    var pendiente = conversacionJson.pendiente;
    var seleccionada = conversacionJson.seleccionada;
    var plantilla = $("#conversacionBoxPlantilla");
    var ultimoMensajeFormateado = pagAppWindow_formateaUltimoMensajeParaConversacionBox(ultimoMensaje);
    var fechaUltimoMensajeFormateada = pagAppWindow_formateaFechaUltimoMensajeParaConversacionBox(fechaUltimoMensaje);

    var conversacionBox = plantilla.clone(true);
    conversacionBox.attr("id",prefijoIdBloque_conversacionBox + aliasRemitente);
    conversacionBox.removeClass("d-none");    
        
    var aliasRemitenteElement = conversacionBox.find(".conversacionBoxRemitente");
    aliasRemitenteElement.text(aliasRemitente);   
    var ultimoMensajeElement = conversacionBox.find(".conversacionBoxUltimoMensajeTexto");
    ultimoMensajeElement.text(ultimoMensajeFormateado);  
    var fechaUltimoMensajeElement = conversacionBox.find(".conversacionBoxUltimoMensajeFecha");
    fechaUltimoMensajeElement.text(fechaUltimoMensajeFormateada);  
    if (seleccionada == true){
        conversacionBox.addClass("conversacionBoxAbierta");
    } else{
        conversacionBox.removeClass("conversacionBoxAbierta");
    }

    return conversacionBox;
}

function pagAppWindow_formateaFechaUltimoMensajeParaConversacionBox(fechaUltimoMensajeMiliseconds){
    var fechaUltimoMensaje = new Date(fechaUltimoMensajeMiliseconds);
    var hoy = new Date();
    var fechaUltimoMensajeFormateada = "";

    if(fechaUltimoMensaje.getDate() == hoy.getDate() && fechaUltimoMensaje.getMonth() == hoy.getMonth() && fechaUltimoMensaje.getFullYear() == hoy.getFullYear()) {
        fechaUltimoMensajeFormateada = fechaUltimoMensaje.getHours() + ":" + fechaUltimoMensaje.getMinutes();
    }else{
        fechaUltimoMensajeFormateada = fechaUltimoMensaje.getDate() + "/" + (fechaUltimoMensaje.getMonth()+1).toString() + "/" + fechaUltimoMensaje.getFullYear();
    }
    return fechaUltimoMensajeFormateada;
}

function pagAppWindow_formateaUltimoMensajeParaConversacionBox(mensaje){
    var mensajeFormateado = mensaje.split(": ");
    mensajeFormateado.shift();
    mensajeFormateado = mensajeFormateado.join(": ");
    if (mensajeFormateado.length > const_MAXLENGTHMENSAJECONVERSACIONBOX){
        mensajeFormateado = mensajeFormateado.substring(0,const_MAXLENGTHMENSAJECONVERSACIONBOX-string_PUNTOSSUSPENSIVOS.length)+string_PUNTOSSUSPENSIVOS;
    }

    return mensajeFormateado;
}

function onPageReady(){
    javaConnector.obtenerListaConversacionesAbiertas(null);
}

/*
$(function(){
    pagAppWindow_refrescarListaConversacionesAbiertas(JSON.parse(mockup_jsonConversaciones));

})*/