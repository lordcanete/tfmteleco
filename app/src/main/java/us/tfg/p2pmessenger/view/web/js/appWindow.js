var idCapaAgenda ="#pagAppWindow_capaAgenda"

function ocultarElemento(elementoId){
    $(elementoId).addClass("d-none");
    $(elementoId).removeClass("d-flex");
}

function mostrarElemento(elementoId){
    $(elementoId).addClass("d-flex");
    $(elementoId).removeClass("d-none");
}

function ocultarCapaAgenda(){
    ocultarElemento(idCapaAgenda);
}

function mostrarCapaAgenda(){
    mostrarElemento(idCapaAgenda);
}