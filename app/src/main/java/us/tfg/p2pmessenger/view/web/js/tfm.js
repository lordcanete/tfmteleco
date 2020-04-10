var jsConnector = {
    notificarError: function(mensajeError){
        mostrarBloqueNotificacion(idBloque_notifError, idBloque_textoError, mensajeError);
    },
    comprobarEstadoCallback: function () {
        javaConnector.comprobarEstado();
    },
    abrirPanelAgenda: function(listaContactosJsonString){
        var listaContactosJson = JSON.parse(listaContactosJsonString);
        pagAppWindow_limpiarFormularioNuevoContacto();
        pagAppWindow_mostrarCapaAgenda(listaContactosJson);
    },
    actualizarPanelAgenda: function(){
        pagAppWindow_limpiarFormularioNuevoContacto();
        pagAppWindow_mostrarAgendaActualizada();
    },
    actualizarPanelConversaciones: function(listaConversacionesJsonString){
        var listaConversacionesJSON = JSON.parse(listaConversacionesJsonString);
        pagAppWindow_refrescarListaConversacionesAbiertas(listaConversacionesJSON);
    }
};

function getJsConnector() {
    return jsConnector;
};

function mostrarBloqueNotificacion(idBloque, idUbicacionMensaje = null, mensaje = null){
    if (idUbicacionMensaje != null && mensaje != null){
        $(idUbicacionMensaje).text(mensaje);
    }

    $(idBloque).addClass("d-flex");
    $(idBloque).removeClass("d-none");
}

function ocultarBloqueNotificacion(idBloque){
    $(idBloque).addClass("d-none");
    $(idBloque).removeClass("d-flex");
}

function limpiarTextoInput(id){
    $(id).val("");
}