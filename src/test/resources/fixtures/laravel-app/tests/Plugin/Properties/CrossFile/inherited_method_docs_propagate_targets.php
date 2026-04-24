<?php

use App\Support\UserPropertiesProvider;

$value = (new UserPropertiesProvider())->getProps()['name'];
