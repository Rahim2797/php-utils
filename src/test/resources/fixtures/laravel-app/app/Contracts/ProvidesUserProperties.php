<?php

namespace App\Contracts;

use Rahim2797\MagicTypes\Properties;

interface ProvidesUserProperties
{
    /**
     * @return Properties<\App\Models\User>
     */
    public function getProps(): array;
}
