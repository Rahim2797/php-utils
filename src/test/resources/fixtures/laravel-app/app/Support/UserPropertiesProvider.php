<?php

namespace App\Support;

use App\Contracts\ProvidesUserProperties;

class UserPropertiesProvider implements ProvidesUserProperties
{
    public function getProps(): array
    {
        return [];
    }
}
