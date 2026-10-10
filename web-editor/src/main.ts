import { mount } from "svelte";
import App from "./App.svelte";
import "./app.css";
import { setDecoder } from "./lib/store/audio";
import { decodeFile } from "./lib/audio/browser";

// lines of opened packs are stored as Ogg files and decoded when first needed
setDecoder((ogg) => decodeFile(ogg.slice().buffer));

export default mount(App, { target: document.getElementById("app")! });
