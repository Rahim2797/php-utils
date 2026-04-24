<?php

use App\Support\PropertySelectionService;

$properties = (new PropertySelectionService())->getProductOrAccountProperties();
$label = $properties['<caret>'];
