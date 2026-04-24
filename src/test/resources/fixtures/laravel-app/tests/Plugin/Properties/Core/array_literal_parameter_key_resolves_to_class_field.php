<?php

use App\Support\PropertiesApi;

(new PropertiesApi())->takesUserProperties([
    'ema<caret>il' => 'hello@example.com',
]);
