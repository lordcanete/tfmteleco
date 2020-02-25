var idCapaAgenda ="#pagAppWindow_capaAgenda"

function ocultarElemento(elementoId){
    $(elementoId).addClass("d-none");
}

function mostrarElemento(elementoId){
    $(elementoId).removeClass("d-none");
}

function ocultarCapaAgenda(){
    ocultarElemento(idCapaAgenda);
}

function mostrarCapaAgenda(){
    mostrarElemento(idCapaAgenda);
}