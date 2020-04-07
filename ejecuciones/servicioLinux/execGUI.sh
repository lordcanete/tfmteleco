#!/bin/bash

java -cp "../../lib/*" --module-path /usr/share/openjfx/lib --add-modules=javafx.controls,javafx.web us.tfg.p2pmessenger.view.VistaGUI $1
