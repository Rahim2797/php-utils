<?php

namespace App\Support;

use Rahim2797\MagicTypes\Properties;

class PropertiesApi
{
    /**
     * @return Properties<\App\Models\User>
     */
    public function getUserProperties(): array
    {
        return [];
    }

    /**
     * @param Properties<\App\Models\User> $props
     */
    public function takesUserProperties(array $props): void
    {
    }
}
