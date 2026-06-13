#!/bin/bash

echo "Generating feature graphics to ~/nasux-icons/nasux-feature-graphic.png..."
mkdir -p ~/nasux-icons/
rsvg-convert feature-graphic.svg > ~/nasux-icons/feature-graphic.png
