<?php

use App\Support\PropertiesApi;

$value = (new PropertiesApi())->getUserProperties()['name'];
