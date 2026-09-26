const isDarkMode = window.matchMedia('(prefers-color-scheme: dark)').matches;

window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', event=>{
    if(event.matches){
        console.log("El usuario cambio a modo oscuro");
    }else{
        console.log("El usuario cambio a modo claro");
    }
})