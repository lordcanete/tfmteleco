var idCapaCargando ="#capaCargando";
var jsConnector = {
    notificarError: function(mensajeError){
        ocultarBloque(idCapaCargando);
        mostrarBloqueNotificacion(idBloque_notifError, idBloque_textoError, mensajeError);
    },
    comprobarEstadoCallback: function () {
        javaConnector.comprobarEstado();
    },
    abrirPanelAgenda: function(listaContactosJsonString){
        var listaContactosJson = JSON.parse(listaContactosJsonString);
        pagAppWindow_limpiarFormularioNuevoContacto();        
        pagAppWindow_limpiarFormularioNuevoGrupo();
        pagAppWindow_mostrarCapaAgenda(listaContactosJson);
    },
    actualizarPanelAgenda: function(){
        ocultarBloque(idCapaCargando);
        pagAppWindow_limpiarFormularioNuevoContacto();
        pagAppWindow_mostrarAgendaActualizada();
    },
    actualizarPanelConversaciones: function(listaConversacionesJsonString){
        var listaConversacionesJSON = JSON.parse(listaConversacionesJsonString);
        ocultarBloque(idCapaCargando);
        pagAppWindow_refrescarListaConversacionesAbiertas(listaConversacionesJSON);
    },
    actualizarPanelConversacionSeleccionada: function(listaMensajesJsonString, aliasRemitente, esGrupo){
        var listaMensajesJSON = JSON.parse(listaMensajesJsonString);
        ocultarBloque(idCapaCargando);
        pagAppWindow_refrescarPanelConversacionSeleccionada(listaMensajesJSON, aliasRemitente, esGrupo);
    },
    actualizarPanelesAppWindowTrasEnvioMensaje: function(aliasRemitente){
        pagAppWindow_limpiarFormularioNuevoMensaje();
        javaConnector.obtenerListaConversacionesAbiertas(aliasRemitente);
    },
    actualizarPanelesAppWindowTrasNotificacion: function(aliasRemitente=null){
        javaConnector.obtenerListaConversacionesAbiertas(aliasRemitente);
    },
    actualizarCodigoInvitacionGrupo: function(codigo){
        pagAppWindow_refrescarCodigoInvitacionGrupo(codigo);
    },
    actualizarPanelConversacionesTrasCrearGrupo: function(aliasRemitente=null){    
        ocultarBloque(idCapaCargando);
        pagAppWindow_limpiarFormularioNuevoGrupo();
        javaConnector.obtenerListaConversacionesAbiertas(aliasRemitente);
    },
    actualizarPanelConversacionesTrasUnirseAGrupo: function(aliasRemitente=null){
        pagAppWindow_limpiarFormularioUnirseAGrupo();
        javaConnector.obtenerListaConversacionesAbiertas(aliasRemitente);
    }
};

function getJsConnector() {
    return jsConnector;
};

function mostrarBloqueNotificacion(idBloque, idUbicacionMensaje = null, mensaje = null){
    if (idUbicacionMensaje != null && mensaje != null){
        $(idUbicacionMensaje).text(mensaje);
    }
    mostrarBloque(idBloque);
}

function ocultarBloqueNotificacion(idBloque){
    ocultarBloque(idBloque);
}

function mostrarBloque(idBloque){
    $(idBloque).addClass("d-flex");
    $(idBloque).removeClass("d-none");
}

function ocultarBloque(idBloque){
    $(idBloque).addClass("d-none");
    $(idBloque).removeClass("d-flex");
}

function limpiarTextoInput(id){
    $(id).val("");
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

$(function () {
    $('[data-toggle="tooltip"]').tooltip();
  })