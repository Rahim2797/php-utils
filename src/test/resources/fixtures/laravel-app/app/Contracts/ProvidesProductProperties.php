<?php

namespace App\Contracts;

use Rahim2797\MagicTypes\Properties;

interface ProvidesProductProperties
{
    /**
     * @return Properties<\App\Models\Product>
     */
    public function getProductProperties(): array;
}
