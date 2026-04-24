<?php

use App\Support\PropertiesApi;

(new PropertiesApi())->takesUserProperties([
    'na<caret>me' => 'Ada',
]);
